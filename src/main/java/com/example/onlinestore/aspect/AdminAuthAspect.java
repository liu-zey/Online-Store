package com.example.onlinestore.aspect;

import com.example.onlinestore.context.UserContext;
import com.example.onlinestore.model.User;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment; // Added import
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
// Removed @Value import
import org.springframework.stereotype.Component;

/**
 * 管理员权限验证切面
 * 
 * 该切面用于处理带有 @RequireAdmin 注解的方法的权限验证。
 * 它会检查当前用户是否为管理员，如果不是，将抛出异常并返回本地化的错误消息。
 * 
 * 配置示例：
 * <pre>
 * admin:
 *   auth:
 *     username: admin
 * </pre>
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @RequireAdmin
 * public ResponseEntity<?> adminOnlyMethod() {
 *     // 方法实现
 * }
 * }
 * </pre>
 */
@Aspect
@Component
public class AdminAuthAspect {
    private static final Logger logger = LoggerFactory.getLogger(AdminAuthAspect.class);

    private final MessageSource messageSource;
    private final Environment environment; // Added Environment field

    @Autowired // Constructor injection
    public AdminAuthAspect(MessageSource messageSource, Environment environment) {
        this.messageSource = messageSource;
        this.environment = environment;
    }

    /**
     * 检查当前用户是否具有管理员权限
     * 
     * @throws IllegalArgumentException 如果用户未登录或不是管理员
     */
    @Before("@annotation(com.example.onlinestore.annotation.RequireAdmin)")
    public void checkAdminAuth() {
        User currentUser = UserContext.getCurrentUser();
        String adminUsername = environment.getProperty("admin.auth.username"); // Get from Environment

        if (currentUser == null) {
            logger.warn("访问被拒绝：未登录用户尝试访问管理员接口");
            throw new IllegalArgumentException(messageSource.getMessage(
                "error.access.denied", null, LocaleContextHolder.getLocale()));
        }
        
        if (!currentUser.getUsername().equals(adminUsername)) { // Check against username from environment
            logger.warn("访问被拒绝：非管理员用户 {} 尝试访问管理员接口", currentUser.getUsername());
            throw new IllegalArgumentException(messageSource.getMessage(
                "error.access.denied", null, LocaleContextHolder.getLocale()));
        }
        
        logger.debug("管理员 {} 访问接口成功", currentUser.getUsername());
    }
} 