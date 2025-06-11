package com.example.onlinestore.model;

import java.time.LocalDateTime;
import java.util.HashSet; // Uncommented
import java.util.Set;   // Uncommented

public class Role {
    private Long id;
    private String name; // e.g., ROLE_ADMIN, ROLE_PRODUCT_MANAGER
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // For MyBatis, we might not map direct collections here
    // if join tables are handled in XML. These are for conceptual model.
    // private Set<User> users = new HashSet<>(); // Keep users commented for now unless explicitly needed by UserDetails
    private Set<Permission> permissions = new HashSet<>(); // Uncommented

    // Constructors
    public Role() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Role(String name, String description) {
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
    public Set<Permission> getPermissions() { return permissions; } // Uncommented
    public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; } // Uncommented
    // public Set<User> getUsers() { return users; }
    // public void setUsers(Set<User> users) { this.users = users; }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return id != null ? id.equals(role.id) : role.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Role{" + "id=" + id + ", name='" + name + '\'' + '}';
    }
}
