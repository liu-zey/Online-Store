package com.example.onlinestore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects; // For Objects.hash

public class User {
    private Long id;
    private String username;
    @JsonIgnore
    private String password; // Essential for UserDetails

    private String token; // Related to custom token auth, might be refactored with Spring Security
    private LocalDateTime tokenExpireTime; // Related to custom token auth

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // private boolean isAdmin; // To be replaced by roles system

    private Set<Role> roles = new HashSet<>();

    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public User(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getTokenExpireTime() {
        return tokenExpireTime;
    }

    public void setTokenExpireTime(LocalDateTime tokenExpireTime) {
        this.tokenExpireTime = tokenExpireTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    // public boolean isAdmin() { return isAdmin; }
    // public void setAdmin(boolean admin) { isAdmin = admin; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) &&
               Objects.equals(username, user.username); // Typically equals/hashCode based on business key
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username); // Match fields used in equals
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                // Do not include password in toString
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
