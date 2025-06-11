package com.example.onlinestore.controller;

import com.example.onlinestore.dto.ProductCreateRequest;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;


@WebMvcTest(ProductController.class)
public class ProductControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addProduct_whenValidRequest_shouldReturnCreatedProduct() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("New Gadget");
        request.setDescription("The latest and greatest gadget.");
        request.setPrice(new BigDecimal("129.99"));

        Product mockProduct = new Product();
        mockProduct.setId(1L); // Assume ID is set after creation
        mockProduct.setName(request.getName());
        mockProduct.setDescription(request.getDescription());
        mockProduct.setPrice(request.getPrice());
        mockProduct.setCreatedAt(LocalDateTime.now());

        when(productService.addProduct(any(ProductCreateRequest.class))).thenReturn(mockProduct);

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Then
        verify(productService, times(1)).addProduct(any(ProductCreateRequest.class));

        resultActions.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(mockProduct.getId().intValue())))
                .andExpect(jsonPath("$.name", is(mockProduct.getName())))
                .andExpect(jsonPath("$.description", is(mockProduct.getDescription())))
                .andExpect(jsonPath("$.price", is(mockProduct.getPrice().doubleValue())));
                // Note: LocalDateTime might need custom serializer/deserializer or string comparison
                // For simplicity, we are not asserting createdAt here, but it can be done.
    }

    @Test
    void addProduct_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
        // Given
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName(null); // Name is @NotBlank
        request.setPrice(new BigDecimal("0.00")); // Price is @DecimalMin("0.01")

        // When
        ResultActions resultActions = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Then
        // Verify productService.addProduct is NOT called due to validation failure
        verify(productService, times(0)).addProduct(any(ProductCreateRequest.class));

        resultActions.andExpect(status().isBadRequest());
        // Further assertions can be made on the error response body if a global exception handler is configured
        // to return specific error formats (e.g., checking for field errors).
    }
}
