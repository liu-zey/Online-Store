package com.example.onlinestore.controller;

// import com.example.onlinestore.annotation.RequireAdmin; // Removed
import com.example.onlinestore.annotation.ValidateParams;
// ErrorResponse DTO might not be directly returned now if GlobalExceptionHandler handles it
// import com.example.onlinestore.dto.ErrorResponse;
import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.dto.UserPageRequest;
import com.example.onlinestore.dto.UserVO;
import com.example.onlinestore.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Added
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private MessageSource messageSource;

    /**
     * 获取用户列表
     * 
     * @param request 分页请求参数
     * @return 用户列表分页数据
     */
    @GetMapping
    // @RequireAdmin // Removed
    @PreAuthorize("hasRole('SUPER_ADMIN')") // Added
    @ValidateParams
    public ResponseEntity<PageResponse<UserVO>> listUsers(@Valid UserPageRequest request) {
        // Exception handling for service layer calls will now be managed by GlobalExceptionHandler
        // So, try-catch blocks for IllegalArgumentException or generic Exception might be removed
        // if they just reformat to ErrorResponse, as GlobalExceptionHandler will do that.
        // However, specific logging or metrics collection within the catch might still be valid.
        // For now, let's assume GlobalExceptionHandler handles the response formatting.
        // The controller method can be simplified to just call the service.

        logger.debug("Attempting to list users with request: {}", request);
        PageResponse<UserVO> response = userService.listUsers(request);
        logger.debug("Successfully listed users, {} records found.", response.getRecords().size());
        return ResponseEntity.ok(response);
        // Old try-catch removed for brevity, assuming GlobalExceptionHandler handles exceptions from userService.listUsers
    }
} 