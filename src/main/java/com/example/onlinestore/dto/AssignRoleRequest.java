package com.example.onlinestore.dto;

import jakarta.validation.constraints.NotNull;

public class AssignRoleRequest {

    @NotNull(message = "User ID cannot be null")
    private Long userId;

    @NotNull(message = "Role ID cannot be null")
    private Long roleId;

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
