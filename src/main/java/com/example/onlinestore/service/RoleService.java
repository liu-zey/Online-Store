package com.example.onlinestore.service;

import com.example.onlinestore.model.Role;
import com.example.onlinestore.model.Permission; // For future methods
import java.util.List;
import java.util.Set;

public interface RoleService {
    Role createRole(Role role);
    Role findByName(String name);
    Role findById(Long id);
    List<Role> findAllRoles();
    // void deleteRole(Long roleId); // Will add later
    // Role addPermissionToRole(Long roleId, Long permissionId); // Will add later
    // Role removePermissionFromRole(Long roleId, Long permissionId); // Will add later
    // Set<Permission> getPermissionsForRole(Long roleId); // Will add later
}
