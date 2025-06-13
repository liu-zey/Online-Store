package com.example.onlinestore.mapper;

import com.example.onlinestore.model.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
// import java.util.Set; // Not strictly needed here if permissions don't directly link back to roles in their own mapper methods

@Mapper
public interface PermissionMapper {

    Permission findById(@Param("id") Long id);

    Permission findByName(@Param("name") String name);

    List<Permission> findAll();

    void insertPermission(Permission permission);

    void updatePermission(Permission permission);

    void deletePermission(@Param("id") Long id);

    // If needed in the future, a method to find all permissions for a given role ID
    // could be added here, though it's already in RoleMapper.
    // Set<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);
}
