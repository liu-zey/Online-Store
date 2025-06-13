package com.example.onlinestore.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import com.example.onlinestore.dto.LoginRequest;
import com.example.onlinestore.dto.LoginResponse;
import com.example.onlinestore.mapper.RoleMapper; // Added
import com.example.onlinestore.mapper.UserMapper;
import com.example.onlinestore.model.Role; // Added
import com.example.onlinestore.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
// import static org.mockito.ArgumentMatchers.anyString; // Already present
import java.util.HashSet; // Added
import java.util.Set; // Added

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private Environment environment;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private UserMapper userMapper;
    @Mock
    private MessageSource messageSource;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private CircuitBreakerFactory circuitBreakerFactory;
    @Mock
    private CircuitBreaker circuitBreaker;
    @Mock
    private RoleMapper roleMapper; // Added

    @InjectMocks
    private UserServiceImpl userService;

    private static final String TOKEN_PREFIX = "token:";
    private static final String EXTERNAL_USER_SERVICE_CB_NAME = "externalUserService";
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        when(objectMapper.registerModule(any())).thenReturn(objectMapper);
        // Common setup for circuit breaker
        when(circuitBreakerFactory.create(EXTERNAL_USER_SERVICE_CB_NAME)).thenReturn(circuitBreaker);
    }

    // Helper to easily mock circuit breaker execution
    private <T> Answer<T> executeSupplierOrFallback(boolean shouldSupplierRun) {
        return invocation -> {
            Supplier<T> supplier = invocation.getArgument(0);
            Function<Throwable, T> fallback = invocation.getArgument(1);
            if (shouldSupplierRun) {
                return supplier.get();
            } else {
                return fallback.apply(new RuntimeException("Simulated error for fallback execution"));
            }
        };
    }

    @Test
    @DisplayName("Admin login success")
    void login_adminSuccess() throws Exception {
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password");

        when(environment.getProperty("admin.auth.username")).thenReturn("admin");
        when(environment.getProperty("admin.auth.password")).thenReturn("password");
        when(environment.getProperty("service.user.base-url")).thenReturn("http://localhost:8080/users");

        // Mock userMapper behavior
        when(userMapper.findByUsername("admin")).thenReturn(null); // Assume new admin user for simplicity
        // Mock Redis behavior via objectMapper for storing user
        User mockUser = new User();
        mockUser.setUsername("admin");
        when(objectMapper.writeValueAsString(any(User.class))).thenReturn("{\"username\":\"admin\"}");


        LoginResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getExpireTime());
        verify(userMapper).insertUser(any(User.class)); // Or updateUserToken if user exists
        verify(redisTemplate).opsForValue().set(eq(TOKEN_PREFIX + response.getToken()), anyString(), eq(1L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("Admin login failure - wrong password")
    void login_adminWrongPassword() {
        loginRequest.setUsername("admin");
        loginRequest.setPassword("wrongpassword");

        when(environment.getProperty("admin.auth.username")).thenReturn("admin");
        when(environment.getProperty("admin.auth.password")).thenReturn("password");
        when(messageSource.getMessage(eq("error.invalid.credentials"), any(), any(Locale.class)))
            .thenReturn("Invalid credentials");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.login(loginRequest);
        });

        assertEquals("Invalid credentials", exception.getMessage());
        verify(userMapper, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Regular user login success via Circuit Breaker")
    void login_regularUserSuccess_viaCircuitBreaker() throws Exception {
        loginRequest.setUsername("user");
        loginRequest.setPassword("password");

        User externalUserResponse = new User();
        externalUserResponse.setUsername("user");

        when(environment.getProperty("admin.auth.username")).thenReturn("different_admin");
        when(environment.getProperty("service.user.base-url")).thenReturn("http://ext-service.com");

        // Mock circuitBreaker.run() to execute the supplier (actual call)
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
            .thenAnswer(executeSupplierOrFallback(true));

        when(restTemplate.postForObject(eq("http://ext-service.com/auth/login"), any(LoginRequest.class), eq(User.class)))
            .thenReturn(externalUserResponse);

        when(userMapper.findByUsername("user")).thenReturn(null); // New user
        when(objectMapper.writeValueAsString(any(User.class))).thenReturn("{\"username\":\"user\"}");

        LoginResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertNotNull(response.getToken());
        verify(userMapper).insertUser(any(User.class));
        verify(redisTemplate).opsForValue().set(eq(TOKEN_PREFIX + response.getToken()), anyString(), eq(1L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("Regular user login failure - external service fails, fallback executed")
    void login_regularUserFailure_fallbackExecuted() {
        loginRequest.setUsername("user");
        loginRequest.setPassword("password");

        when(environment.getProperty("admin.auth.username")).thenReturn("different_admin");
        when(environment.getProperty("service.user.base-url")).thenReturn("http://ext-service.com");
        when(messageSource.getMessage(eq("error.userService.unavailable"), any(), any(Locale.class)))
            .thenReturn("External service unavailable");

        // Mock circuitBreaker.run() to execute the fallback
        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
            .thenAnswer(executeSupplierOrFallback(false));

        // We don't need to mock restTemplate.postForObject here as the fallback should be triggered before that.
        // Or, if the supplier is called and throws an exception:
        // when(restTemplate.postForObject(anyString(), any(), eq(User.class))).thenThrow(new RestClientException("Connection failed"));


        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.login(loginRequest);
        });

        assertTrue(exception.getMessage().contains("External service unavailable"));
    }

    @Test
    @DisplayName("Regular user login - external service returns null user")
    void login_regularUser_externalUserIsNull() {
        loginRequest.setUsername("user");
        loginRequest.setPassword("password");

        when(environment.getProperty("admin.auth.username")).thenReturn("different_admin");
        when(environment.getProperty("service.user.base-url")).thenReturn("http://ext-service.com");
        when(messageSource.getMessage(eq("error.user.notFound"), any(), any(Locale.class)))
            .thenReturn("User not found from external service");

        when(circuitBreaker.run(any(Supplier.class), any(Function.class)))
            .thenAnswer(executeSupplierOrFallback(true));
        when(restTemplate.postForObject(eq("http://ext-service.com/auth/login"), any(LoginRequest.class), eq(User.class)))
            .thenReturn(null); // External service returns null

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.login(loginRequest);
        });

        assertEquals("User not found from external service", exception.getMessage());
    }

    @Test
    @DisplayName("Regular user login - service URL not configured")
    void login_regularUser_serviceUrlNotConfigured() {
        loginRequest.setUsername("user");
        loginRequest.setPassword("password");

        when(environment.getProperty("admin.auth.username")).thenReturn("different_admin");
        when(environment.getProperty("service.user.base-url")).thenReturn(""); // Empty or null
        when(messageSource.getMessage(eq("error.userService.notConfigured"), any(), any(Locale.class)))
            .thenReturn("User service URL not configured");

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.login(loginRequest);
        });

        assertEquals("User service URL not configured", exception.getMessage());
        verify(circuitBreakerFactory, never()).create(anyString()); // Circuit breaker should not be created
    }


    @Test
    @DisplayName("Logout should delete token from Redis when token is valid")
    void logout_shouldDeleteTokenFromRedis_whenTokenIsValid() {
        String token = "valid-token";
        String redisKey = TOKEN_PREFIX + token;

        userService.logout(token);

        verify(redisTemplate).delete(redisKey);
    }

    @Test
    @DisplayName("Logout should not attempt to delete when token is null")
    void logout_shouldNotAttemptToDelete_whenTokenIsNull() {
        userService.logout(null);

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("Logout should not attempt to delete when token is empty")
    void logout_shouldNotAttemptToDelete_whenTokenIsEmpty() {
        userService.logout("");
        // If token is "", (token != null && !token.isEmpty()) is (true && false) = false.
        // So, redisTemplate.delete() should not be called.
        verify(redisTemplate, never()).delete(anyString());
    }

    // Tests for assignRoleToUser and revokeRoleFromUser

    @Test
    @DisplayName("Assign role to user successfully")
    void assignRoleToUser_success() {
        User user = new User("testuser", "password");
        user.setId(1L);
        Role role = new Role("ROLE_USER", "User role");
        role.setId(10L);

        User updatedUserWithRole = new User("testuser", "password");
        updatedUserWithRole.setId(1L);
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        updatedUserWithRole.setRoles(roles);

        when(userMapper.findById(1L)).thenReturn(user).thenReturn(updatedUserWithRole);
        when(roleMapper.findById(10L)).thenReturn(role);
        // messageSource mock for getMessage is already part of the class from previous tests
        // No need to mock insertUserRole as it's a void method, just verify it's called.

        User result = userService.assignRoleToUser(1L, 10L);

        verify(userMapper).insertUserRole(1L, 10L);
        assertNotNull(result);
        assertEquals(updatedUserWithRole, result); // Verifies re-fetch logic
        assertTrue(result.getRoles().contains(role)); // Check if role is in the set
    }

    @Test
    @DisplayName("Assign role to user fails if user not found")
    void assignRoleToUser_userNotFound_throwsIllegalArgumentException() {
        when(userMapper.findById(1L)).thenReturn(null);
        when(messageSource.getMessage(eq("error.user.notFound.id"), any(Object[].class), any(Locale.class)))
            .thenReturn("User not found with id: 1");


        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.assignRoleToUser(1L, 10L);
        });
        assertTrue(exception.getMessage().contains("User not found with id: 1"));
        verify(roleMapper, never()).findById(anyLong());
        verify(userMapper, never()).insertUserRole(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Assign role to user fails if role not found")
    void assignRoleToUser_roleNotFound_throwsIllegalArgumentException() {
        User user = new User("testuser", "password");
        user.setId(1L);
        when(userMapper.findById(1L)).thenReturn(user);
        when(roleMapper.findById(10L)).thenReturn(null);
        when(messageSource.getMessage(eq("error.role.notFound.id"), any(Object[].class), any(Locale.class)))
            .thenReturn("Role not found with id: 10");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.assignRoleToUser(1L, 10L);
        });
        assertTrue(exception.getMessage().contains("Role not found with id: 10"));
        verify(userMapper, never()).insertUserRole(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Revoke role from user successfully")
    void revokeRoleFromUser_success() {
        User user = new User("testuser", "password");
        user.setId(1L);
        Role role = new Role("ROLE_USER", "User role");
        role.setId(10L);
        Set<Role> initialRoles = new HashSet<>();
        initialRoles.add(role);
        user.setRoles(initialRoles); // User initially has the role

        User userAfterRoleRemoval = new User("testuser", "password");
        userAfterRoleRemoval.setId(1L);
        userAfterRoleRemoval.setRoles(new HashSet<>()); // Roles set is empty after removal

        when(userMapper.findById(1L)).thenReturn(user).thenReturn(userAfterRoleRemoval);
        // No need to mock deleteUserRole as it's void, just verify.

        User result = userService.revokeRoleFromUser(1L, 10L);

        verify(userMapper).deleteUserRole(1L, 10L);
        assertNotNull(result);
        assertEquals(userAfterRoleRemoval, result); // Verifies re-fetch
        assertTrue(result.getRoles().isEmpty()); // Check roles are empty
    }

    @Test
    @DisplayName("Revoke role from user fails if user not found")
    void revokeRoleFromUser_userNotFound_throwsIllegalArgumentException() {
        when(userMapper.findById(1L)).thenReturn(null);
         when(messageSource.getMessage(eq("error.user.notFound.id"), any(Object[].class), any(Locale.class)))
            .thenReturn("User not found with id: 1");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.revokeRoleFromUser(1L, 10L);
        });
        assertTrue(exception.getMessage().contains("User not found with id: 1"));
        verify(userMapper, never()).deleteUserRole(anyLong(), anyLong());
    }
}
