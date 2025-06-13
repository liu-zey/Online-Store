package com.example.onlinestore.service.impl;

import com.example.onlinestore.mapper.UserMapper;
import com.example.onlinestore.model.User;
import com.example.onlinestore.model.Role;
import com.example.onlinestore.model.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment; // Added import
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
// import java.util.stream.Collectors; // Not strictly needed with current getAuthorities logic

@Service("userDetailsService") // Explicit bean name can be useful
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final Environment environment; // Added

    @Autowired
    public UserDetailsServiceImpl(UserMapper userMapper, Environment environment) { // Added Environment
        this.userMapper = userMapper;
        this.environment = environment; // Added
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.trim().isEmpty()) {
            throw new UsernameNotFoundException("Username cannot be empty");
        }

        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                true,               // isEnabled() -> Assuming true
                true,               // accountNonExpired -> Assuming true
                true,               // credentialsNonExpired -> Assuming true
                true,               // accountNonLocked -> Assuming true
                getAuthorities(user) // Pass the whole User object
        );
    }

    private Set<GrantedAuthority> getAuthorities(User user) { // Modified to take User
        Set<GrantedAuthority> authorities = new HashSet<>();
        // Add authorities from database-assigned roles and their permissions
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role.getName() != null && !role.getName().trim().isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority(role.getName()));
                }
                if (role.getPermissions() != null) {
                    for (Permission permission : role.getPermissions()) {
                        if (permission.getName() != null && !permission.getName().trim().isEmpty()) {
                            authorities.add(new SimpleGrantedAuthority(permission.getName()));
                        }
                    }
                }
            }
        }
        // Explicitly add ROLE_SUPER_ADMIN if the user is the configured admin user
        String adminUsername = environment.getProperty("admin.auth.username");
        if (adminUsername != null && adminUsername.equals(user.getUsername())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        return authorities;
    }
}
