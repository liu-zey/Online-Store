package com.example.onlinestore.service.impl;

import com.example.onlinestore.mapper.RoleMapper;
import com.example.onlinestore.model.Role;
import com.example.onlinestore.service.RoleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleMapper roleMapper;

    @Autowired
    public RoleServiceImpl(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Transactional
    @Override
    public Role createRole(Role role) {
        Assert.notNull(role, "Role object must not be null");
        Assert.hasText(role.getName(), "Role name must not be empty");

        // Check if role already exists by name
        if (roleMapper.findByName(role.getName()) != null) {
            log.warn("Role with name '{}' already exists.", role.getName());
            // Or throw a custom exception e.g., DuplicateRoleException
            throw new IllegalArgumentException("Role with name '" + role.getName() + "' already exists.");
        }
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.insertRole(role);
        log.info("Created new role with id: {}, name: {}", role.getId(), role.getName());
        return role;
    }

    @Override
    public Role findByName(String name) {
        Assert.hasText(name, "Role name must not be empty");
        return roleMapper.findByName(name);
    }

    @Override
    public Role findById(Long id) {
        Assert.notNull(id, "Role ID must not be null");
        return roleMapper.findById(id);
    }

    @Override
    public List<Role> findAllRoles() {
        return roleMapper.findAll();
    }
}
