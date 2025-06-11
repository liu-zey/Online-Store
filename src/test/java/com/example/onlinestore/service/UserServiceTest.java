package com.example.onlinestore.service;

import com.example.onlinestore.dto.LoginRequest;
import com.example.onlinestore.dto.LoginResponse;
import com.example.onlinestore.model.User;
import com.example.onlinestore.mapper.UserMapper;
import com.example.onlinestore.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

// Add these imports to UserServiceTest.java
import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.dto.UserPageRequest;
import com.example.onlinestore.dto.UserVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "password";
    private static final String USER_SERVICE_BASE_URL = "http://user-service";

    @BeforeEach
    void setUp() {
        // 设置配置项的值
        ReflectionTestUtils.setField(userService, "adminUsername", ADMIN_USERNAME);
        ReflectionTestUtils.setField(userService, "adminPassword", ADMIN_PASSWORD);
        ReflectionTestUtils.setField(userService, "userServiceBaseUrl", USER_SERVICE_BASE_URL);
    }

    @Test
    void whenAdminLoginWithNewUser_thenCreateUserAndReturnToken() {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setUsername(ADMIN_USERNAME);
        request.setPassword(ADMIN_PASSWORD);

        // 设置mock行为：用户不存在
        when(userMapper.findByUsername(ADMIN_USERNAME)).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 执行测试
        LoginResponse response = userService.login(request);

        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getExpireTime());
        
        // 验证调用
        verify(userMapper).findByUsername(ADMIN_USERNAME);
        verify(userMapper).insertUser(any(User.class));
        verify(userMapper, never()).updateUserToken(any(User.class));
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
        
        // 验证没有调用用户服务
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void whenAdminLoginWithExistingUser_thenUpdateTokenAndReturn() {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setUsername(ADMIN_USERNAME);
        request.setPassword(ADMIN_PASSWORD);

        // 设置mock行为：用户已存在
        User existingUser = new User();
        existingUser.setUsername(ADMIN_USERNAME);
        existingUser.setToken("old-token");
        existingUser.setTokenExpireTime(LocalDateTime.now().minusDays(1));
        when(userMapper.findByUsername(ADMIN_USERNAME)).thenReturn(existingUser);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 执行测试
        LoginResponse response = userService.login(request);

        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getExpireTime());
        assertNotEquals("old-token", response.getToken());
        
        // 验证调用
        verify(userMapper).findByUsername(ADMIN_USERNAME);
        verify(userMapper, never()).insertUser(any(User.class));
        verify(userMapper).updateUserToken(any(User.class));
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
        
        // 验证没有调用用户服务
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void whenNormalUserLoginWithNewUser_thenCreateUserAndReturnToken() {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setUsername("normal_user");
        request.setPassword("password");

        // 设置mock行为：用户不存在，认证成功
        when(userMapper.findByUsername("normal_user")).thenReturn(null);
        when(restTemplate.postForObject(eq(USER_SERVICE_BASE_URL + "/auth"), any(), eq(Boolean.class)))
            .thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 执行测试
        LoginResponse response = userService.login(request);

        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getExpireTime());
        
        // 验证调用
        verify(userMapper).findByUsername("normal_user");
        verify(userMapper).insertUser(any(User.class));
        verify(userMapper, never()).updateUserToken(any(User.class));
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
        verify(restTemplate).postForObject(eq(USER_SERVICE_BASE_URL + "/auth"), any(), eq(Boolean.class));

        // 验证插入的用户数据
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insertUser(userCaptor.capture());
        User insertedUser = userCaptor.getValue();
        assertEquals("normal_user", insertedUser.getUsername());
        assertEquals(response.getToken(), insertedUser.getToken());
        assertEquals(response.getExpireTime(), insertedUser.getTokenExpireTime());
    }

    @Test
    void whenNormalUserLoginWithExistingUser_thenUpdateTokenAndReturn() {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setUsername("normal_user");
        request.setPassword("password");

        // 设置mock行为：用户已存在，认证成功
        User existingUser = new User();
        existingUser.setUsername("normal_user");
        existingUser.setToken("old-token");
        existingUser.setTokenExpireTime(LocalDateTime.now().minusDays(1));
        when(userMapper.findByUsername("normal_user")).thenReturn(existingUser);
        when(restTemplate.postForObject(eq(USER_SERVICE_BASE_URL + "/auth"), any(), eq(Boolean.class)))
            .thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // 执行测试
        LoginResponse response = userService.login(request);

        // 验证结果
        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getExpireTime());
        assertNotEquals("old-token", response.getToken());
        
        // 验证调用
        verify(userMapper).findByUsername("normal_user");
        verify(userMapper, never()).insertUser(any(User.class));
        verify(userMapper).updateUserToken(any(User.class));
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any());
        verify(restTemplate).postForObject(eq(USER_SERVICE_BASE_URL + "/auth"), any(), eq(Boolean.class));

        // 验证更新的用户数据
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateUserToken(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertEquals("normal_user", updatedUser.getUsername());
        assertEquals(response.getToken(), updatedUser.getToken());
        assertEquals(response.getExpireTime(), updatedUser.getTokenExpireTime());
    }

    @Test
    void whenAdminLoginWithWrongPassword_thenThrowException() {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setUsername(ADMIN_USERNAME);
        request.setPassword("wrong_password");

        // 设置错误消息
        when(messageSource.getMessage(eq("error.invalid.credentials"), isNull(), any(Locale.class)))
            .thenReturn("Invalid username or password");

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> userService.login(request));
        assertEquals("Invalid username or password", exception.getMessage());
        
        // 验证调用
        verify(userMapper, never()).findByUsername(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
        verify(userMapper, never()).updateUserToken(any(User.class));
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
        verify(restTemplate, never()).postForObject(anyString(), any(), any());
    }

    @Test
    void whenNormalUserLoginWithWrongPassword_thenThrowException() {
        // 准备测试数据
        LoginRequest request = new LoginRequest();
        request.setUsername("normal_user");
        request.setPassword("wrong_password");

        // 设置mock行为
        when(restTemplate.postForObject(eq(USER_SERVICE_BASE_URL + "/auth"), any(), eq(Boolean.class)))
            .thenReturn(false);
        when(messageSource.getMessage(eq("error.invalid.credentials"), isNull(), any(Locale.class)))
            .thenReturn("Invalid username or password");

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> userService.login(request));
        assertEquals("Invalid username or password", exception.getMessage());
        
        // 验证调用
        verify(userMapper, never()).findByUsername(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
        verify(userMapper, never()).updateUserToken(any(User.class));
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
        verify(restTemplate).postForObject(eq(USER_SERVICE_BASE_URL + "/auth"), any(), eq(Boolean.class));
    }

    // --- Tests for listUsers ---
    @Test
    void listUsers_whenNoUsers_shouldReturnEmptyPage() {
        UserPageRequest request = new UserPageRequest();
        request.setPageNum(1);
        request.setPageSize(10);

        when(userMapper.findAllWithPagination(0, 10)).thenReturn(Collections.emptyList());
        when(userMapper.countTotal()).thenReturn(0L);

        PageResponse<UserVO> response = userService.listUsers(request);

        assertNotNull(response);
        assertTrue(response.getRecords().isEmpty());
        assertEquals(0, response.getTotal());
        assertEquals(1, response.getPageNum());
        assertEquals(10, response.getPageSize());

        verify(userMapper).findAllWithPagination(0, 10);
        verify(userMapper).countTotal();
    }

    @Test
    void listUsers_whenUsersExist_shouldReturnPaginatedResults() {
        UserPageRequest request = new UserPageRequest();
        request.setPageNum(1);
        request.setPageSize(1);

        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setCreatedAt(LocalDateTime.now().minusDays(1));
        user1.setUpdatedAt(LocalDateTime.now());

        when(userMapper.findAllWithPagination(0, 1)).thenReturn(Arrays.asList(user1));
        when(userMapper.countTotal()).thenReturn(1L);

        PageResponse<UserVO> response = userService.listUsers(request);

        assertNotNull(response);
        assertEquals(1, response.getRecords().size());
        assertEquals(1L, response.getTotal());
        assertEquals(1, response.getPageNum());
        assertEquals(1, response.getPageSize());
        assertEquals("user1", response.getRecords().get(0).getUsername());
        assertEquals(user1.getId(), response.getRecords().get(0).getId());

        verify(userMapper).findAllWithPagination(0, 1);
        verify(userMapper).countTotal();
    }

    // --- Tests for getUserByToken ---
    @Test
    void getUserByToken_whenTokenExistsAndValidJson_shouldReturnUser() throws Exception {
        String token = "valid-token";
        User expectedUser = new User();
        expectedUser.setId(1L);
        expectedUser.setUsername("testuser");
        // UserServiceImpl creates its own ObjectMapper, so we need to simulate what it would produce
        ObjectMapper internalMapper = new ObjectMapper();
        internalMapper.registerModule(new JavaTimeModule());
        String userJson = internalMapper.writeValueAsString(expectedUser);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token:" + token)).thenReturn(userJson);

        User actualUser = userService.getUserByToken(token);

        assertNotNull(actualUser);
        assertEquals(expectedUser.getId(), actualUser.getId());
        assertEquals(expectedUser.getUsername(), actualUser.getUsername());
        verify(valueOperations).get("token:" + token);
    }

    @Test
    void getUserByToken_whenTokenNotFoundInRedis_shouldReturnNull() {
        String token = "non-existent-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token:" + token)).thenReturn(null);

        User actualUser = userService.getUserByToken(token);

        assertNull(actualUser);
        verify(valueOperations).get("token:" + token);
    }

    @Test
    void getUserByToken_whenRedisThrowsException_shouldReturnNullAndLogError() {
        String token = "error-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token:" + token)).thenThrow(new RuntimeException("Redis connection failed"));

        User actualUser = userService.getUserByToken(token);

        assertNull(actualUser);
        // Verification of logging would require a spy or a custom appender, skip for now if complex
        verify(valueOperations).get("token:" + token);
    }

    @Test
    void getUserByToken_whenInvalidJsonInRedis_shouldReturnNullAndLogError() {
        String token = "invalid-json-token";
        String invalidJson = "{\"id\":1, \"username\":\"testuser\","; // Malformed JSON

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token:" + token)).thenReturn(invalidJson);

        User actualUser = userService.getUserByToken(token);

        assertNull(actualUser);
        // Verification of logging would require a spy or a custom appender
        verify(valueOperations).get("token:" + token);
    }

    // --- Tests for error handling in createLoginResponse (via login method) ---
    @Test
    void login_whenRedisSetThrowsException_shouldStillReturnLoginResponseAndLogError() {
        LoginRequest request = new LoginRequest();
        request.setUsername(ADMIN_USERNAME);
        request.setPassword(ADMIN_PASSWORD);

        User existingUser = new User();
        existingUser.setUsername(ADMIN_USERNAME);
        when(userMapper.findByUsername(ADMIN_USERNAME)).thenReturn(existingUser);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Simulate Redis set operation failing
        doThrow(new RuntimeException("Redis write failed")).when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        LoginResponse response = userService.login(request);

        assertNotNull(response, "LoginResponse should not be null even if Redis caching fails");
        assertNotNull(response.getToken());
        assertNotNull(response.getExpireTime());

        verify(userMapper).updateUserToken(any(User.class)); // Ensure user update was still attempted
        verify(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        // Verification of logging would require a spy or a custom appender
    }

    // Note: Testing ObjectMapper exceptions from within createLoginResponse is harder
    // because UserServiceImpl instantiates its own ObjectMapper.
    // To test that, UserServiceImpl would ideally take ObjectMapper as a dependency.
    // The current test for getUserByToken_whenInvalidJsonInRedis_shouldReturnNullAndLogError
    // indirectly tests the readValue failure for getUserByToken.
} 