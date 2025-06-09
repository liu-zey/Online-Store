package com.example.onlinestore.interceptor;

import com.example.onlinestore.context.UserContext;
import com.example.onlinestore.exception.UnauthorizedException; // Added import
import com.example.onlinestore.model.User;
import com.example.onlinestore.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private MessageSource messageSource;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("X-Token");
        if (token == null || token.trim().isEmpty()) { // Also check for empty token
            // Get localized message for missing token
            String message = messageSource.getMessage("error.auth.missingToken", null, LocaleContextHolder.getLocale());
            throw new UnauthorizedException(message);
        }

        User user = userService.getUserByToken(token);
        if (user == null) {
            // Get localized message for invalid token
            String message = messageSource.getMessage("error.auth.invalidToken", null, LocaleContextHolder.getLocale());
            throw new UnauthorizedException(message);
        }

        UserContext.setCurrentUser(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
} 