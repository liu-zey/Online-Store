package com.example.onlinestore.service.impl;

import com.example.onlinestore.mapper.UserMapper;
import com.example.onlinestore.model.User;
import com.example.onlinestore.model.Role;
import com.example.onlinestore.model.Permission; // Though not directly used in these specific tests, good for context
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private Environment environment;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;
    private final String configuredAdminUsername = "configAdmin";

    @BeforeEach
    void setUp() {
        testUser = new User("testuser", "password123");
        testUser.setId(1L);

        // Common environment mocking
        when(environment.getProperty("admin.auth.username")).thenReturn(configuredAdminUsername);
    }

    @Test
    @DisplayName("Load regular user by username - success")
    void loadUserByUsername_regularUser_success() {
        Role customerRole = new Role("ROLE_CUSTOMER", "Customer Role");
        // Simulating permissions being part of the Role object, fetched by MyBatis
        Permission viewProductsPermission = new Permission("product:view", "View products");
        Set<Permission> customerPermissions = new HashSet<>();
        customerPermissions.add(viewProductsPermission);
        customerRole.setPermissions(customerPermissions); // Assume Role has setPermissions

        Set<Role> roles = new HashSet<>();
        roles.add(customerRole);
        testUser.setRoles(roles);

        when(userMapper.findByUsername("testuser")).thenReturn(testUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals(testUser.getPassword(), userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_CUSTOMER")));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("product:view")));
        assertFalse(userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPER_ADMIN")));
    }

    @Test
    @DisplayName("Load admin user by username - gets ROLE_SUPER_ADMIN")
    void loadUserByUsername_adminUser_hasSuperAdminRole() {
        User adminUser = new User(configuredAdminUsername, "securepassword");
        adminUser.setId(2L);
        // Admin might also have other roles from DB, e.g., ROLE_STAFF
        Role staffRole = new Role("ROLE_STAFF", "Staff Role");
        Set<Role> adminDbRoles = new HashSet<>();
        adminDbRoles.add(staffRole);
        adminUser.setRoles(adminDbRoles);


        when(userMapper.findByUsername(configuredAdminUsername)).thenReturn(adminUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername(configuredAdminUsername);

        assertNotNull(userDetails);
        assertEquals(configuredAdminUsername, userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_SUPER_ADMIN")),
                "Admin user should have ROLE_SUPER_ADMIN");
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_STAFF")),
                "Admin user should also retain their database-assigned roles like ROLE_STAFF");
    }

    @Test
    @DisplayName("Load user not found - throws UsernameNotFoundException")
    void loadUserByUsername_notFound_throwsException() {
        when(userMapper.findByUsername("unknownuser")).thenReturn(null);

        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("unknownuser");
        });
        assertEquals("User not found with username: unknownuser", exception.getMessage());
    }

    @Test
    @DisplayName("Load user with null username - throws UsernameNotFoundException")
    void loadUserByUsername_nullUsername_throwsException() {
        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(null);
        });
        assertEquals("Username cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("Load user with empty username - throws UsernameNotFoundException")
    void loadUserByUsername_emptyUsername_throwsException() {
        Exception exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("  ");
        });
        assertEquals("Username cannot be empty", exception.getMessage());
    }

    @Test
    @DisplayName("User with no DB roles but is admin - gets ROLE_SUPER_ADMIN")
    void loadUserByUsername_adminUser_noDbRoles_hasSuperAdminRole() {
        User adminUser = new User(configuredAdminUsername, "securepassword");
        adminUser.setId(2L);
        adminUser.setRoles(new HashSet<>()); // No roles from DB

        when(userMapper.findByUsername(configuredAdminUsername)).thenReturn(adminUser);

        UserDetails userDetails = userDetailsService.loadUserByUsername(configuredAdminUsername);

        assertNotNull(userDetails);
        assertEquals(configuredAdminUsername, userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_SUPER_ADMIN")),
                "Admin user should have ROLE_SUPER_ADMIN even with no DB roles");
        assertEquals(1, userDetails.getAuthorities().size(), "Admin user should only have ROLE_SUPER_ADMIN if no other roles/permissions from DB");
    }
}
