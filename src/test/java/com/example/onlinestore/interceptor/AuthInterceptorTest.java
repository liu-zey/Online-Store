package com.example.onlinestore.interceptor;

import com.example.onlinestore.context.UserContext;
import com.example.onlinestore.exception.UnauthorizedException;
import com.example.onlinestore.model.User;
import com.example.onlinestore.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthInterceptor 测试")
class AuthInterceptorTest {

    @Mock
    private UserService userService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Object handler;

    @InjectMocks
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void setUp() {
        // Clear context before each test
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Clear context after each test
        UserContext.clear();
    }

    @Test
    @DisplayName("Token缺失 - 抛出UnauthorizedException")
    void preHandle_missingToken_shouldThrowUnauthorizedException() {
        when(request.getHeader("X-Token")).thenReturn(null);
        when(messageSource.getMessage(eq("error.auth.missingToken"), any(), any(Locale.class)))
            .thenReturn("Missing token");

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authInterceptor.preHandle(request, response, handler);
        });

        assertEquals("Missing token", exception.getMessage());
        verify(userService, never()).getUserByToken(anyString());
        assertNull(UserContext.getCurrentUser());
    }

    @Test
    @DisplayName("Token为空字符串 - 抛出UnauthorizedException")
    void preHandle_emptyToken_shouldThrowUnauthorizedException() {
        when(request.getHeader("X-Token")).thenReturn(" "); // Test with a blank token
        when(messageSource.getMessage(eq("error.auth.missingToken"), any(), any(Locale.class)))
            .thenReturn("Missing token"); // Assuming blank is treated as missing

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authInterceptor.preHandle(request, response, handler);
        });

        assertEquals("Missing token", exception.getMessage());
        verify(userService, never()).getUserByToken(anyString());
        assertNull(UserContext.getCurrentUser());
    }

    @Test
    @DisplayName("Token无效 (userService返回null) - 抛出UnauthorizedException")
    void preHandle_invalidToken_shouldThrowUnauthorizedException() {
        String invalidToken = "invalid-token";
        when(request.getHeader("X-Token")).thenReturn(invalidToken);
        when(userService.getUserByToken(invalidToken)).thenReturn(null);
        when(messageSource.getMessage(eq("error.auth.invalidToken"), any(), any(Locale.class)))
            .thenReturn("Invalid token");

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            authInterceptor.preHandle(request, response, handler);
        });

        assertEquals("Invalid token", exception.getMessage());
        assertNull(UserContext.getCurrentUser());
    }

    @Test
    @DisplayName("Token有效 - 设置UserContext并返回true")
    void preHandle_validToken_shouldSetUserContextAndReturnTrue() throws Exception {
        String validToken = "valid-token";
        User mockUser = new User();
        mockUser.setUsername("testuser");

        when(request.getHeader("X-Token")).thenReturn(validToken);
        when(userService.getUserByToken(validToken)).thenReturn(mockUser);

        boolean result = authInterceptor.preHandle(request, response, handler);

        assertTrue(result);
        assertNotNull(UserContext.getCurrentUser());
        assertEquals("testuser", UserContext.getCurrentUser().getUsername());
    }

    @Test
    @DisplayName("afterCompletion - 清除UserContext")
    void afterCompletion_shouldClearUserContext() {
        User mockUser = new User();
        UserContext.setCurrentUser(mockUser); // Simulate user being set
        assertNotNull(UserContext.getCurrentUser());

        authInterceptor.afterCompletion(request, response, handler, null);

        assertNull(UserContext.getCurrentUser());
    }
}
