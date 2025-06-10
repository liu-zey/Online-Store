package com.example.onlinestore.interceptor;

import com.example.onlinestore.context.UserContext;
import com.example.onlinestore.model.User;
import com.example.onlinestore.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private UserService userService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private AuthInterceptor authInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        // Default locale for message source
        // when(messageSource.getMessage(eq("error.unauthorized"), any(), any(Locale.class)))
        //         .thenReturn("Unauthorized access"); // Moved to specific tests
    }

    @AfterEach
    void tearDown() {
        UserContext.clear(); // Ensure context is cleared after each test
    }

    @Test
    void preHandle_validToken_shouldReturnTrueAndSetUserContext() throws Exception {
        String token = "valid-token";
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        request.addHeader("X-Token", token);
        when(userService.getUserByToken(token)).thenReturn(mockUser);

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertTrue(result);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus()); // Should not be SC_UNAUTHORIZED
        assertNotNull(UserContext.getCurrentUser());
        assertEquals(mockUser.getId(), UserContext.getCurrentUser().getId());
        verify(userService).getUserByToken(token);
    }

    @Test
    void preHandle_missingToken_shouldReturnFalseAndSetUnauthorized() throws Exception {
        when(messageSource.getMessage(eq("error.unauthorized"), any(), any(Locale.class)))
                .thenReturn("Unauthorized access");
        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unauthorized access"));
        assertNull(UserContext.getCurrentUser());
        verify(userService, never()).getUserByToken(anyString());
    }

    @Test
    void preHandle_invalidToken_shouldReturnFalseAndSetUnauthorized() throws Exception {
        when(messageSource.getMessage(eq("error.unauthorized"), any(), any(Locale.class)))
                .thenReturn("Unauthorized access");
        String token = "invalid-token";
        request.addHeader("X-Token", token);
        when(userService.getUserByToken(token)).thenReturn(null);

        boolean result = authInterceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unauthorized access"));
        assertNull(UserContext.getCurrentUser());
        verify(userService).getUserByToken(token);
    }

    @Test
    void preHandle_userServiceThrowsException_shouldPropagateException() throws Exception {
        String token = "valid-token";
        request.addHeader("X-Token", token);
        when(userService.getUserByToken(token)).thenThrow(new RuntimeException("DB error"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            authInterceptor.preHandle(request, response, new Object());
        });

        assertEquals("DB error", exception.getMessage());
        assertNull(UserContext.getCurrentUser()); // Context should not be set
        // Response status might be set by a global exception handler, not directly by the interceptor in this case
    }


    @Test
    void afterCompletion_shouldClearUserContext() throws Exception {
        User mockUser = new User();
        UserContext.setCurrentUser(mockUser); // Simulate user being set

        authInterceptor.afterCompletion(request, response, new Object(), null);

        assertNull(UserContext.getCurrentUser());
    }

    @Test
    void afterCompletion_withException_shouldStillClearUserContext() throws Exception {
        User mockUser = new User();
        UserContext.setCurrentUser(mockUser); // Simulate user being set

        authInterceptor.afterCompletion(request, response, new Object(), new Exception("Some error"));

        assertNull(UserContext.getCurrentUser());
    }
}
