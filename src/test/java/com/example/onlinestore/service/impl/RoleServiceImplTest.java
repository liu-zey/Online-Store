package com.example.onlinestore.service.impl;

import com.example.onlinestore.mapper.RoleMapper;
import com.example.onlinestore.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private Role sampleRole;

    @BeforeEach
    void setUp() {
        sampleRole = new Role("ROLE_TEST", "Test Role");
        sampleRole.setId(1L);
        sampleRole.setCreatedAt(LocalDateTime.now());
        sampleRole.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createRole_success() {
        Role newRole = new Role("ROLE_NEW", "New Role");
        when(roleMapper.findByName("ROLE_NEW")).thenReturn(null);
        // Mocking for void method that has side effect (ID generation)
        // This assumes insertRole in Mybatis XML has useGeneratedKeys="true" keyProperty="id"
         doAnswer(invocation -> {
             Role r = invocation.getArgument(0);
             r.setId(2L); // Simulate ID generation by DB
             return null; // Void method
         }).when(roleMapper).insertRole(any(Role.class));


        Role created = roleService.createRole(newRole);

        assertNotNull(created);
        assertEquals("ROLE_NEW", created.getName());
        assertNotNull(created.getId()); // Check if ID was set
        verify(roleMapper).insertRole(any(Role.class));
    }

    @Test
    void createRole_alreadyExists_throwsIllegalArgumentException() {
        when(roleMapper.findByName("ROLE_EXISTING")).thenReturn(sampleRole);

        Role existingRole = new Role("ROLE_EXISTING", "Existing Role");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            roleService.createRole(existingRole);
        });
        assertEquals("Role with name 'ROLE_EXISTING' already exists.", exception.getMessage());
        verify(roleMapper, never()).insertRole(any(Role.class));
    }

    @Test
    void findByName_found() {
        when(roleMapper.findByName("ROLE_TEST")).thenReturn(sampleRole);
        Role found = roleService.findByName("ROLE_TEST");
        assertNotNull(found);
        assertEquals("ROLE_TEST", found.getName());
    }

    @Test
    void findByName_notFound_returnsNull() {
        when(roleMapper.findByName("ROLE_UNKNOWN")).thenReturn(null);
        Role found = roleService.findByName("ROLE_UNKNOWN");
        assertNull(found);
    }

    @Test
    void findById_found() {
        when(roleMapper.findById(1L)).thenReturn(sampleRole);
        Role found = roleService.findById(1L);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void findAllRoles_returnsList() {
        List<Role> roles = Arrays.asList(sampleRole, new Role("ROLE_ANOTHER", "Another"));
        when(roleMapper.findAll()).thenReturn(roles);
        List<Role> foundRoles = roleService.findAllRoles();
        assertEquals(2, foundRoles.size());
    }
}
