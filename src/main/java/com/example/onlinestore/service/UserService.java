package com.example.onlinestore.service;

import com.example.onlinestore.dto.LoginRequest;
import com.example.onlinestore.dto.LoginResponse;
import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.dto.UserPageRequest;
import com.example.onlinestore.dto.UserVO;
import com.example.onlinestore.model.User;

public interface UserService {
    LoginResponse login(LoginRequest request);
    PageResponse<UserVO> listUsers(UserPageRequest request);
    User getUserByToken(String token);
    void logout(String token);

    User assignRoleToUser(Long userId, Long roleId);
    User revokeRoleFromUser(Long userId, Long roleId);
    // Set<Role> getUserRoles(Long userId); // This can be fetched via User.getRoles() if populated by UserMapper
} 