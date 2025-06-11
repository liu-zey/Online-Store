package com.example.onlinestore.interceptor;

import com.example.onlinestore.context.UserContext;
import com.example.onlinestore.exception.UnauthorizedException; // Added import
import com.example.onlinestore.context.UserContext;
import com.example.onlinestore.exception.UnauthorizedException;
import com.example.onlinestore.model.User;
import com.example.onlinestore.service.UserService;
import com.example.onlinestore.service.impl.UserDetailsServiceImpl; // Added
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // Added
import org.springframework.security.core.context.SecurityContextHolder; // Added
import org.springframework.security.core.userdetails.UserDetails; // Added
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserService userService;
    private final MessageSource messageSource;
    private final UserDetailsServiceImpl userDetailsService; // Added

    @Autowired // Constructor injection
    public AuthInterceptor(UserService userService, MessageSource messageSource, UserDetailsServiceImpl userDetailsService) {
        this.userService = userService;
        this.messageSource = messageSource;
        this.userDetailsService = userDetailsService;
    }

    // Helper method to get localized messages (assuming it might not be in this class directly)
    // Or if it is, ensure it's defined. For now, assuming messageSource is used directly.
    private String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("X-Token");
        if (token == null || token.trim().isEmpty()) {
            SecurityContextHolder.clearContext(); // Clear context for safety
            throw new UnauthorizedException(getMessage("error.auth.missingToken"));
        }

        User user = userService.getUserByToken(token); // Custom User model from token
        if (user == null) {
            SecurityContextHolder.clearContext(); // Clear context for safety
            throw new UnauthorizedException(getMessage("error.auth.invalidToken"));
        }

        // Load Spring Security UserDetails
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

        // Create Authentication token
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userDetails,
            null, // Credentials are not needed as token is already validated
            userDetails.getAuthorities()
        );

        // Set Authentication in SecurityContextHolder
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Keep custom UserContext if still needed by other parts of application
        UserContext.setCurrentUser(user);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear(); // Existing UserContext clearing
        // SecurityContextHolder is typically cleared by Spring Security's filter chain.
    }
} 