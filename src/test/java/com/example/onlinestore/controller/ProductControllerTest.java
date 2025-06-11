package com.example.onlinestore.controller;

import com.example.onlinestore.config.SecurityConfig;
import com.example.onlinestore.dto.PageResponse;
import com.example.onlinestore.dto.ProductCreateRequest;
import com.example.onlinestore.dto.ProductUpdateRequest;
import com.example.onlinestore.interceptor.AuthInterceptor;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.service.ProductService;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class) // Import security config to process @PreAuthorize
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private AuthInterceptor authInterceptor;

    @Autowired
    private ObjectMapper objectMapper;

    private Product sampleProduct;
    private ProductCreateRequest createRequest;
    private ProductUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Laptop");
        sampleProduct.setPrice(new BigDecimal("1200.50"));
        sampleProduct.setStatus("ACTIVE");

        createRequest = new ProductCreateRequest();
        createRequest.setName("New Laptop");
        createRequest.setPrice(new BigDecimal("1000.00"));
        createRequest.setStockQuantity(10);
        createRequest.setCategoryId(1L);

        updateRequest = new ProductUpdateRequest();
        updateRequest.setName("Updated Laptop");
    }

    // Admin Endpoints
    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void createProduct_asAdmin_returnsCreated() throws Exception {
        when(productService.createProduct(any(Product.class), any())).thenReturn(sampleProduct);
        mockMvc.perform(post("/api/admin/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void updateProduct_asAdmin_returnsOk() throws Exception {
        when(productService.updateProduct(anyLong(), any(Product.class), any())).thenReturn(sampleProduct);
        mockMvc.perform(put("/api/admin/products/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @WithMockUser(roles="SUPER_ADMIN")
    void updateProductStatus_asAdmin_returnsOk() throws Exception {
        Map<String, String> statusMap = Collections.singletonMap("status", "INACTIVE");
        // Update sample product to reflect the change for the return mock
        sampleProduct.setStatus("INACTIVE");
        when(productService.updateProductStatus(eq(1L), eq("INACTIVE"))).thenReturn(sampleProduct);

        mockMvc.perform(patch("/api/admin/products/1/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusMap)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }


    // Public Endpoints
    @Test
    void getAvailableProducts_returnsOk() throws Exception {
        PageResponse<Product> pageResponse = new PageResponse<>(Collections.singletonList(sampleProduct), 0, 1, 1);
        when(productService.getAvailableProducts(anyMap(), anyInt(), anyInt())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/products")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Laptop"));
    }

    @Test
    void getProductByIdForCustomer_activeProduct_returnsOk() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleProduct);
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void getProductByIdForCustomer_inactiveProduct_returnsNotFound() throws Exception {
        sampleProduct.setStatus("INACTIVE");
        when(productService.getProductById(1L)).thenReturn(sampleProduct);
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getProductByIdForCustomer_productNotFound_returnsNotFound() throws Exception {
        when(productService.getProductById(1L)).thenReturn(null);
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isNotFound());
    }
}
