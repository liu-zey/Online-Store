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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.dto.UserPageRequest;
import com.example.onlinestore.dto.UserVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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

    @Test
    void whenListUsersWithEmptyList_thenReturnEmptyPageResponse() {
        // Mocking pagination parameters
        int pageNum = 1;
        int pageSize = 10;
        UserPageRequest pageRequest = new UserPageRequest();
        pageRequest.setPageNum(pageNum);
        pageRequest.setPageSize(pageSize);

        // Mocking userMapper behavior
        when(userMapper.findAllWithPagination(anyInt(), anyInt())).thenReturn(new ArrayList<>());
        when(userMapper.countTotal()).thenReturn(0L);

        // Calling the service method
        PageResponse<UserVO> response = userService.listUsers(pageRequest);

        // Assertions
        assertNotNull(response);
        assertTrue(response.getRecords().isEmpty());
        assertEquals(0, response.getTotal());
        assertEquals(pageNum, response.getPageNum());
        assertEquals(pageSize, response.getPageSize());

        // Verifying mock interactions
        verify(userMapper).findAllWithPagination((pageNum - 1) * pageSize, pageSize);
        verify(userMapper).countTotal();
    }

    @Test
    void whenListUsersWithPaginatedList_thenReturnCorrectPageResponse() {
        // Mocking pagination parameters
        int pageNum = 1;
        int pageSize = 2;

        // Creating a sample list of User objects
        List<User> users = new ArrayList<>();
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setCreatedAt(LocalDateTime.now());
        user1.setUpdatedAt(LocalDateTime.now());
        users.add(user1);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setCreatedAt(LocalDateTime.now().minusDays(1));
        user2.setUpdatedAt(LocalDateTime.now().minusHours(1));
        users.add(user2);

        User user3 = new User(); // This user is only for countTotal mock
        user3.setId(3L);
        user3.setUsername("user3");
        user3.setCreatedAt(LocalDateTime.now().minusDays(2));
        user3.setUpdatedAt(LocalDateTime.now().minusHours(2));

        // Mocking userMapper behavior
        when(userMapper.findAllWithPagination(anyInt(), anyInt())).thenReturn(users);
        when(userMapper.countTotal()).thenReturn(3L); // Total 3 users in DB

        UserPageRequest pageRequest = new UserPageRequest();
        pageRequest.setPageNum(pageNum);
        pageRequest.setPageSize(pageSize);

        // Calling the service method
        PageResponse<UserVO> response = userService.listUsers(pageRequest);

        // Assertions
        assertNotNull(response);
        assertEquals(2, response.getRecords().size());
        assertEquals(3L, response.getTotal());
        assertEquals(pageNum, response.getPageNum());
        assertEquals(pageSize, response.getPageSize());

        // Verifying User to UserVO conversion for the first user
        UserVO userVO1 = response.getRecords().get(0);
        assertEquals(user1.getId(), userVO1.getId());
        assertEquals(user1.getUsername(), userVO1.getUsername());
        assertEquals(user1.getCreatedAt(), userVO1.getCreatedAt());
        assertEquals(user1.getUpdatedAt(), userVO1.getUpdatedAt());

        // Verifying User to UserVO conversion for the second user
        UserVO userVO2 = response.getRecords().get(1);
        assertEquals(user2.getId(), userVO2.getId());
        assertEquals(user2.getUsername(), userVO2.getUsername());
        assertEquals(user2.getCreatedAt(), userVO2.getCreatedAt());
        assertEquals(user2.getUpdatedAt(), userVO2.getUpdatedAt());

        // Verifying mock interactions
        verify(userMapper).findAllWithPagination((pageNum - 1) * pageSize, pageSize);
        verify(userMapper).countTotal();
    }

    @Test
    void whenGetUserByTokenWithValidToken_thenReturnUser() throws JsonProcessingException {
        // Prepare test data
        String token = "valid-token";
        User expectedUser = new User();
        expectedUser.setId(1L);
        expectedUser.setUsername("testuser");
        expectedUser.setToken(token);
        expectedUser.setTokenExpireTime(LocalDateTime.now().plusHours(1));
        expectedUser.setCreatedAt(LocalDateTime.now().minusDays(1));
        expectedUser.setUpdatedAt(LocalDateTime.now());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Register JavaTimeModule for LocalDateTime
        String userJson = objectMapper.writeValueAsString(expectedUser);

        // Mock Redis behavior
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token:" + token)).thenReturn(userJson);

        // Call the service method
        User actualUser = userService.getUserByToken(token);

        // Assertions
        assertNotNull(actualUser);
        assertEquals(expectedUser.getId(), actualUser.getId());
        assertEquals(expectedUser.getUsername(), actualUser.getUsername());
        assertEquals(expectedUser.getToken(), actualUser.getToken());
        assertEquals(expectedUser.getTokenExpireTime(), actualUser.getTokenExpireTime());
        assertEquals(expectedUser.getCreatedAt(), actualUser.getCreatedAt());
        assertEquals(expectedUser.getUpdatedAt(), actualUser.getUpdatedAt());

        // Verify mock interactions
        verify(redisTemplate.opsForValue()).get("token:" + token);
    }

    @Test
    void whenGetUserByTokenWithInvalidOrExpiredToken_thenReturnNull() {
        // Prepare test data
        String token = "invalid-or-expired-token";

        // Mock Redis behavior
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token:" + token)).thenReturn(null);

        // Call the service method
        User actualUser = userService.getUserByToken(token);

        // Assertions
        assertNull(actualUser);

        // Verify mock interactions
        verify(redisTemplate.opsForValue()).get("token:" + token);
    }

    @Test
    void whenGetUserByTokenWithDeserializationError_thenReturnNullAndLogError() {
        // Prepare test data
        String token = "token-with-bad-json";
        String badJson = "{\"id\":1, \"username\":\"testuser\", \"tokenExpireTime\":\"not-a-date\"}"; // Invalid date format

        // Mock Redis behavior
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("token:" + token)).thenReturn(badJson);

        // Call the service method
        User actualUser = userService.getUserByToken(token);

        // Assertions
        assertNull(actualUser); // Expect null due to deserialization failure

        // Verify mock interactions
        verify(redisTemplate.opsForValue()).get("token:" + token);
        // Error logging verification would ideally be here if a logger mock was injected.
        // For now, we assume the implementation handles logging of JsonProcessingException.
    }
}