package com.example.onlinestore.mapper;

import com.example.onlinestore.model.Role;
import com.example.onlinestore.model.Permission; // For methods involving permissions
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface RoleMapper {

    Role findById(@Param("id") Long id);

    Role findByName(@Param("name") String name);

    List<Role> findAll();

    void insertRole(Role role);

    void updateRole(Role role);

    void deleteRole(@Param("id") Long id);

    // Methods for managing role-permission relationships
    void insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    void deleteRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    void deleteAllPermissionsForRole(@Param("roleId") Long roleId);

    Set<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);

    // Method to find roles for a given user (will be used by UserMapper)
    Set<Role> findRolesByUserId(@Param("userId") Long userId);
}
