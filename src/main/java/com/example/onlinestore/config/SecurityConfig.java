package com.example.onlinestore.config;

import com.example.onlinestore.interceptor.AuthInterceptor; // Assuming this is still needed for custom token
import com.example.onlinestore.service.impl.UserDetailsServiceImpl; // Your UserDetailsService
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // For custom filters if needed

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true) // Enable method-level security
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthInterceptor authInterceptor; // Keep this if custom token logic is separate from Spring Security auth for now

    @Autowired
    public SecurityConfig(UserDetailsServiceImpl userDetailsService, AuthInterceptor authInterceptor) {
        this.userDetailsService = userDetailsService;
        this.authInterceptor = authInterceptor;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF Configuration
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs

            // Session Management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless sessions

            // Authorization Rules
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/auth/login", "/api/auth/logout").permitAll() // Login and logout are public
                .requestMatchers("/api/products/**", "/api/categories/**").permitAll() // Assuming product browsing is public
                // .requestMatchers("/api/admin/**").hasRole("ADMIN") // Example, will refine with specific permissions later
                .anyRequest().authenticated() // All other requests require authentication
            );

            // If AuthInterceptor is still handling token validation and UserContext:
            // It will run as a separate HandlerInterceptor. Spring Security won't be aware of its authentication by default.
            // For Spring Security's @PreAuthorize etc. to work with the custom token, AuthInterceptor
            // would need to be modified to populate SecurityContextHolder, or a new Spring Security filter
            // would replace/augment its token validation part. This will be addressed in "core authorization logic" step.

            // Example of adding a custom filter (if JWT or similar was used):
            // http.addFilterBefore(customTokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
