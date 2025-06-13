package com.example.onlinestore.model;

import java.time.LocalDateTime;
// import java.util.HashSet;
// import java.util.Set;

public class Permission {
    private Long id;
    private String name; // e.g., product:create, order:edit
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // For MyBatis, direct collections might not be mapped here.
    // private Set<Role> roles = new HashSet<>();

    // Constructors
    public Permission() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Permission(String name, String description) {
        this();
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    // public Set<Role> getRoles() { return roles; }
    // public void setRoles(Set<Role> roles) { this.roles = roles; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Permission permission = (Permission) o;
        return id != null ? id.equals(permission.id) : permission.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Permission{" + "id=" + id + ", name='" + name + '\'' + '}';
    }
}
