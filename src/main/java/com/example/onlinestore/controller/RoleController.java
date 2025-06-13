package com.example.onlinestore.controller;

import com.example.onlinestore.dto.AssignRoleRequest;
import com.example.onlinestore.model.Role;
import com.example.onlinestore.model.User;
import com.example.onlinestore.service.RoleService;
import com.example.onlinestore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/roles") // Base path for role management
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')") // All methods in this controller require SUPER_ADMIN
public class RoleController {

    private final RoleService roleService;
    private final UserService userService;

    @Autowired
    public RoleController(RoleService roleService, UserService userService) {
        this.roleService = roleService;
        this.userService = userService;
    }

    // Endpoint to create a new role
    @PostMapping
    public ResponseEntity<Role> createRole(@Valid @RequestBody Role roleRequest) {
        // Using @Valid here assumes Role model might have validation annotations (e.g. @NotEmpty for name)
        // If not, @Valid might not be strictly necessary on the Role object itself for this creation endpoint,
        // as service layer does Assert.hasText for name.
        // However, it's good practice if Role object is intended to be validated.
        Role createdRole = roleService.createRole(roleRequest);
        return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
    }

    // Endpoint to get all roles
    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleService.findAllRoles();
        return ResponseEntity.ok(roles);
    }

    // Endpoint to get a specific role by ID
    @GetMapping("/{roleId}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long roleId) {
        Role role = roleService.findById(roleId);
        if (role == null) {
            // Consider throwing a ResourceNotFoundException to be handled by GlobalExceptionHandler
            // For now, direct ResponseEntity is fine.
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(role);
    }

    // Endpoint to assign a role to a user
    @PostMapping("/users/assign")
    public ResponseEntity<User> assignRoleToUser(@Valid @RequestBody AssignRoleRequest assignRoleRequest) {
        User updatedUser = userService.assignRoleToUser(assignRoleRequest.getUserId(), assignRoleRequest.getRoleId());
        return ResponseEntity.ok(updatedUser);
    }

    // Endpoint to revoke a role from a user
    @PostMapping("/users/revoke")
    public ResponseEntity<User> revokeRoleFromUser(@Valid @RequestBody AssignRoleRequest assignRoleRequest) {
        User updatedUser = userService.revokeRoleFromUser(assignRoleRequest.getUserId(), assignRoleRequest.getRoleId());
        return ResponseEntity.ok(updatedUser);
    }

    // Future endpoints:
    // PUT /api/admin/roles/{roleId} (Update role)
    // DELETE /api/admin/roles/{roleId} (Delete role)
    // POST /api/admin/roles/{roleId}/permissions (Assign permission to role)
    // DELETE /api/admin/roles/{roleId}/permissions/{permissionId} (Revoke permission from role)
}
