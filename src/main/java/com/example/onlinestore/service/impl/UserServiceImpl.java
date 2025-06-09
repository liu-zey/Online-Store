package com.example.onlinestore.service.impl;

import com.example.onlinestore.dto.LoginRequest;
import com.example.onlinestore.dto.LoginResponse;
import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.dto.UserPageRequest;
import com.example.onlinestore.dto.UserVO;
import com.example.onlinestore.model.User;
import com.example.onlinestore.mapper.UserMapper;
import com.example.onlinestore.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory; // Added import
import org.springframework.core.env.Environment;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException; // Added import
import org.springframework.web.client.HttpServerErrorException; // Added import
import org.springframework.web.client.RestClientException; // Added import
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final ObjectMapper objectMapper;
    private final Environment environment; // Added Environment field
    private final RestTemplate restTemplate;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final MessageSource messageSource;
    private final String userServiceBaseUrl;
    private final CircuitBreakerFactory circuitBreakerFactory; // Added field

    private static final String AUTH_LOGIN_PATH = "/auth/login"; // Changed from /auth to /auth/login
    private static final String TOKEN_PREFIX = "token:";
    private static final long TOKEN_EXPIRE_DAYS = 1;

    @Autowired // Constructor injection
    public UserServiceImpl(Environment environment, RestTemplate restTemplate,
                           UserMapper userMapper, StringRedisTemplate redisTemplate,
                           MessageSource messageSource, ObjectMapper objectMapper,
                           CircuitBreakerFactory circuitBreakerFactory) { // Added circuitBreakerFactory
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
        this.environment = environment;
        this.restTemplate = restTemplate;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.messageSource = messageSource;
        this.userServiceBaseUrl = environment.getProperty("service.user.base-url");
        this.circuitBreakerFactory = circuitBreakerFactory; // Initialize circuitBreakerFactory
    }

    // Helper method to get localized messages
    private String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String adminUsername = environment.getProperty("admin.auth.username");
        User userToLogin;

        if (request.getUsername().equals(adminUsername)) {
            String adminPassword = environment.getProperty("admin.auth.password");
            if (!request.getPassword().equals(adminPassword)) {
                logger.warn("Admin login attempt failed for user: {}", request.getUsername());
                throw new IllegalArgumentException(getMessage("error.invalid.credentials"));
            }
            // Find existing admin user from local DB
            userToLogin = userMapper.findByUsername(adminUsername);
            if (userToLogin == null) {
                // This case implies admin user needs to be created if not found,
                // or an error should be thrown if admin must pre-exist.
                // For now, let's assume admin should pre-exist or be auto-created.
                logger.info("Admin user {} not found locally, creating.", adminUsername);
                userToLogin = new User();
                userToLogin.setUsername(adminUsername);
                // Set other defaults for admin if necessary
                userToLogin.setCreatedAt(LocalDateTime.now());
                userToLogin.setUpdatedAt(LocalDateTime.now());
                userMapper.insertUser(userToLogin);
            }
            logger.info("管理员快速登录: {}", adminUsername);
        } else {
            // External user logic
            String extUserServiceBaseUrl = environment.getProperty("service.user.base-url");
            if (extUserServiceBaseUrl == null || extUserServiceBaseUrl.isEmpty()) {
                logger.error("User service base URL is not configured.");
                throw new RuntimeException(getMessage("error.userService.notConfigured"));
            }

            User externalServiceResponseUser = circuitBreakerFactory.create("externalUserService").run(() -> {
                try {
                    String authUrl = UriComponentsBuilder.fromHttpUrl(extUserServiceBaseUrl)
                                     .path(AUTH_LOGIN_PATH) // Using /auth/login
                                     .toUriString();
                    // External service is expected to return a User-like object upon successful auth
                    return restTemplate.postForObject(authUrl, request, User.class);
                } catch (HttpClientErrorException | HttpServerErrorException e) {
                    logger.warn("Authentication failed with external user service for user {}: {} {}", request.getUsername(), e.getStatusCode(), e.getResponseBodyAsString());
                    throw e; // Re-throw to be caught by circuit breaker as a failure
                } catch (RestClientException e) {
                    logger.error("Error connecting to external user service for user {}: {}", request.getUsername(), e.getMessage());
                    throw e; // Re-throw for circuit breaker
                }
            }, throwable -> {
                // Fallback logic
                logger.warn("External user service call failed for user {} due to: {}. Executing fallback.", request.getUsername(), throwable.getMessage());
                throw new RuntimeException(getMessage("error.userService.unavailable", throwable.getMessage()), throwable);
            });

            if (externalServiceResponseUser == null || externalServiceResponseUser.getUsername() == null) {
                 logger.warn("External user service returned null or user with null username for: {}", request.getUsername());
                throw new RuntimeException(getMessage("error.user.notFound"));
            }

            // Find or create local user based on external response
            userToLogin = userMapper.findByUsername(externalServiceResponseUser.getUsername());
            if (userToLogin == null) {
                logger.info("User {} authenticated externally, creating local record.", externalServiceResponseUser.getUsername());
                userToLogin = new User();
                userToLogin.setUsername(externalServiceResponseUser.getUsername());
                // Map other relevant fields from externalServiceResponseUser to userToLogin if necessary
                // e.g., userToLogin.setEmail(externalServiceResponseUser.getEmail());
                userToLogin.setCreatedAt(LocalDateTime.now());
                userToLogin.setUpdatedAt(LocalDateTime.now());
                userMapper.insertUser(userToLogin);
            } else {
                logger.info("User {} authenticated externally, found local record.", externalServiceResponseUser.getUsername());
                 // Update existing local user with any new info from external service if needed
                userToLogin.setUpdatedAt(LocalDateTime.now());
                // Assuming updateUserToken also updates `updatedAt` or a more general method exists.
                // If not, userMapper.updateUser(userToLogin) might be more appropriate.
                // For now, let's stick to existing mapper methods or assume token update implies activity.
                // The createLoginResponse will handle token generation and saving it to user.
            }
        }
        
        return createLoginResponse(userToLogin); // Pass the resolved User object
    }

    private LoginResponse createLoginResponse(User user) { // Changed param from String username to User user
        String token = UUID.randomUUID().toString();
        LocalDateTime expireTime = LocalDateTime.now().plusDays(TOKEN_EXPIRE_DAYS);

        // Update user's token and expiry time
        user.setToken(token);
        user.setTokenExpireTime(expireTime);
        user.setUpdatedAt(LocalDateTime.now()); // Ensure updatedAt is current

        if (user.getId() == null) { // Should not happen if user is fetched or created before this
             logger.warn("User object passed to createLoginResponse has null ID for username: {}", user.getUsername());
             // This implies the user was not saved to DB prior to this call, which is problematic.
             // However, current UserMapper.insertUser might not populate ID if not configured for auto-gen keys.
             // For safety, re-fetch if ID is null, though this indicates a flaw in prior logic.
             User existingUser = userMapper.findByUsername(user.getUsername());
             if(existingUser != null) user = existingUser;
             else { // If still null, means it wasn't inserted properly or this is a new user path error.
                 logger.error("User {} not found for token generation after supposed insert/find.", user.getUsername());
                 throw new RuntimeException("User record inconsistency for " + user.getUsername());
             }
             // Re-apply token details if we had to re-fetch
             user.setToken(token);
             user.setTokenExpireTime(expireTime);
             user.setUpdatedAt(LocalDateTime.now());
        }

        // Persist token updates
        // If it's a new user, insertUser should have been called.
        // If existing, updateUserToken.
        // Let's assume user object has ID if it's an existing user.
        // The User object coming in should be the one from DB (with ID) or a new one about to be inserted.
        // The previous logic was: findByUsername, if null then insert, else update token.
        // This method should now just receive the prepared User object.
        // The critical part is that `userMapper.updateUserToken(user)` needs a user with an ID.
        // If it's a brand new user, `insertUser` was called. `updateUserToken` would be for existing.
        // The passed 'user' object to this method should be the one that is already persisted or has its ID set if new.
        // The `userMapper.insertUser` in the main login logic should ideally set the ID on the user object.
        // If `user.getId()` is null here for a new user, `updateUserToken` will fail if it relies on ID.
        // Let's refine createLoginResponse to just use the username from the User object
        // and re-fetch to ensure we have the ID for `updateUserToken`. This is safer.

        User userForTokenUpdate = userMapper.findByUsername(user.getUsername());
        if(userForTokenUpdate == null) {
            // This should ideally not happen if user was inserted in the main login flow.
            // This indicates a logical error if a user that was just inserted cannot be found.
            logger.error("CRITICAL: User {} not found right before token update. Aborting.", user.getUsername());
            throw new RuntimeException("Failed to find user " + user.getUsername() + " for token generation.");
        }
        userForTokenUpdate.setToken(token);
        userForTokenUpdate.setTokenExpireTime(expireTime);
        userForTokenUpdate.setUpdatedAt(LocalDateTime.now());
        userMapper.updateUserToken(userForTokenUpdate);
        logger.info("Updated token for user: {}", userForTokenUpdate.getUsername());

        try {
            // Cache the user (now with token info) to Redis
            String redisKey = TOKEN_PREFIX + token;
            // It's better to cache the user object that has the ID and all current fields
            String userJson = objectMapper.writeValueAsString(userForTokenUpdate);
            redisTemplate.opsForValue().set(redisKey, userJson, TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);
            logger.info("User information cached in Redis for user: {}", userForTokenUpdate.getUsername());
        } catch (Exception e) {
            logger.error("Failed to cache user information to Redis for user {}: {}", userForTokenUpdate.getUsername(), e.getMessage());
            // 继续处理，因为这不是致命错误
        }

        // 返回响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpireTime(expireTime);
        return response;
    }

    private UserVO convertToVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }

    @Override
    public PageResponse<UserVO> listUsers(UserPageRequest request) {
        // 计算分页参数
        int offset = (request.getPageNum() - 1) * request.getPageSize();
        int limit = request.getPageSize();

        // 查询数据
        List<User> users = userMapper.findAllWithPagination(offset, limit);
        long total = userMapper.countTotal();

        // 转换为VO
        List<UserVO> userVOs = users.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        // 构建响应
        PageResponse<UserVO> response = new PageResponse<>();
        response.setRecords(userVOs);
        response.setTotal(total);
        response.setPageNum(request.getPageNum());
        response.setPageSize(request.getPageSize());

        return response;
    }

    @Override
    public User getUserByToken(String token) {
        try {
            String redisKey = TOKEN_PREFIX + token;
            String userJson = redisTemplate.opsForValue().get(redisKey);
            if (userJson == null) {
                logger.warn("无效的token: {}", token);
                return null;
            }
            return objectMapper.readValue(userJson, User.class);
        } catch (Exception e) {
            logger.error("从Redis获取用户信息失败", e);
            return null;
        }
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            String redisKey = TOKEN_PREFIX + token;
            redisTemplate.delete(redisKey);
            logger.info("Token deleted from Redis: {}", token);
        }
        // No error is thrown if the token doesn't exist, to prevent information leakage.
    }
} 