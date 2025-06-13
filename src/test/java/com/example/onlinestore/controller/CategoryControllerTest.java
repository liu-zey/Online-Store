package com.example.onlinestore.controller;

import com.example.onlinestore.config.SecurityConfig; // To apply method security
import com.example.onlinestore.interceptor.AuthInterceptor;
import com.example.onlinestore.model.Category;
import com.example.onlinestore.service.CategoryService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class) // Import security config to process @PreAuthorize
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    // Spring Security related beans needed by SecurityConfig
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private AuthInterceptor authInterceptor; // If AuthInterceptor is a bean

    @Autowired
    private ObjectMapper objectMapper;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category();
        sampleCategory.setId(1L);
        sampleCategory.setName("Electronics");
    }

    // Admin endpoints
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createCategory_asAdmin_returnsCreated() throws Exception {
        when(categoryService.createCategory(any(Category.class))).thenReturn(sampleCategory);
        mockMvc.perform(post("/api/admin/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleCategory)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @WithMockUser(roles = "USER") // Not SUPER_ADMIN
    void createCategory_asNonAdmin_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleCategory)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateCategory_asAdmin_returnsOk() throws Exception {
        when(categoryService.updateCategory(anyLong(), any(Category.class))).thenReturn(sampleCategory);
        mockMvc.perform(put("/api/admin/categories/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleCategory)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void deleteCategory_asAdmin_returnsNoContent() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);
        mockMvc.perform(delete("/api/admin/categories/1")
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    // Public endpoints
    @Test
    void getAllCategoriesForCustomer_returnsOk() throws Exception {
        List<Category> categories = Arrays.asList(sampleCategory);
        when(categoryService.getAllCategories()).thenReturn(categories);
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    void getCategoryByIdForCustomer_found_returnsOk() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(sampleCategory);
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void getCategoryByIdForCustomer_notFound_returnsNotFound() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(null);
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isNotFound());
    }
}
