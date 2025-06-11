package com.example.onlinestore.controller;

import com.example.onlinestore.config.SecurityConfig; // Importing to ensure @EnableMethodSecurity is active
import com.example.onlinestore.dto.AssignRoleRequest;
import com.example.onlinestore.interceptor.AuthInterceptor;
import com.example.onlinestore.model.Role;
import com.example.onlinestore.model.User;
import com.example.onlinestore.service.RoleService;
import com.example.onlinestore.service.UserService;
import com.example.onlinestore.service.impl.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
// import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf; // CSRF is disabled
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
// Import SecurityConfig to make sure @EnableMethodSecurity is processed.
// This also means UserDetailsServiceImpl and AuthInterceptor (if it's a bean used in SecurityConfig) need to be mocked.
@Import(SecurityConfig.class)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoleService roleService;

    @MockBean
    private UserService userService;

    // UserDetailsServiceImpl is a dependency of SecurityConfig
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    // AuthInterceptor is also a dependency of SecurityConfig
    @MockBean
    private AuthInterceptor authInterceptor;

    @Autowired
    private ObjectMapper objectMapper;

    private Role sampleRole;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleRole = new Role("ROLE_ADMIN", "Admin Role");
        sampleRole.setId(1L);

        sampleUser = new User("testUser", "password");
        sampleUser.setId(1L);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createRole_whenSuperAdmin_thenSuccess() throws Exception {
        when(roleService.createRole(any(Role.class))).thenReturn(sampleRole);

        mockMvc.perform(post("/api/admin/roles")
                // .with(csrf()) // CSRF is disabled in our SecurityConfig
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRole)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createRole_whenNotSuperAdmin_thenForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/roles")
                // .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRole)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRole_whenUnauthenticated_thenUnauthorized() throws Exception {
         mockMvc.perform(post("/api/admin/roles")
                // .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleRole)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getAllRoles_whenSuperAdmin_thenSuccess() throws Exception {
        List<Role> roles = Arrays.asList(sampleRole);
        when(roleService.findAllRoles()).thenReturn(roles);

        mockMvc.perform(get("/api/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getRoleById_whenSuperAdminAndFound_thenSuccess() throws Exception {
        when(roleService.findById(1L)).thenReturn(sampleRole);
        mockMvc.perform(get("/api/admin/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ROLE_ADMIN"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void getRoleById_whenSuperAdminAndNotFound_thenNotFound() throws Exception {
        when(roleService.findById(anyLong())).thenReturn(null);
        mockMvc.perform(get("/api/admin/roles/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void assignRoleToUser_whenSuperAdmin_thenSuccess() throws Exception {
        AssignRoleRequest request = new AssignRoleRequest();
        request.setUserId(1L);
        request.setRoleId(1L);
        when(userService.assignRoleToUser(1L, 1L)).thenReturn(sampleUser);

        mockMvc.perform(post("/api/admin/roles/users/assign")
                // .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUser"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void revokeRoleFromUser_whenSuperAdmin_thenSuccess() throws Exception {
        AssignRoleRequest request = new AssignRoleRequest();
        request.setUserId(1L);
        request.setRoleId(1L);
        when(userService.revokeRoleFromUser(1L, 1L)).thenReturn(sampleUser);

        mockMvc.perform(post("/api/admin/roles/users/revoke")
                // .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUser"));
    }
}
