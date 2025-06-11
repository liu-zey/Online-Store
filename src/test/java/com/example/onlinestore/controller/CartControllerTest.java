package com.example.onlinestore.controller;

import com.example.onlinestore.config.SecurityConfig; // To apply method security
import com.example.onlinestore.context.UserContext;
import com.example.onlinestore.dto.AddItemToCartRequest;
import com.example.onlinestore.dto.CartDto;
import com.example.onlinestore.dto.UpdateCartItemRequest;
import com.example.onlinestore.interceptor.AuthInterceptor;
import com.example.onlinestore.model.User;
import com.example.onlinestore.service.CartService;
import com.example.onlinestore.service.impl.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser; // Standard way for WebMvcTest
// import org.springframework.security.core.context.SecurityContextHolder; // For manual context setting if needed

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@Import(SecurityConfig.class) // Import security config to process @PreAuthorize
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    // Spring Security related beans needed by SecurityConfig
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private AuthInterceptor authInterceptor; // If AuthInterceptor is a bean

    @Autowired
    private ObjectMapper objectMapper;

    private CartDto sampleCartDto;
    private User mockUser; // For UserContext

    @BeforeEach
    void setUp() {
        sampleCartDto = new CartDto();
        sampleCartDto.setUserId(1L); // Example User ID

        // Mock UserContext if CartController relies on it directly
        // For @PreAuthorize("isAuthenticated()"), Spring Security's context is primary.
        // If getCurrentUserId() in controller explicitly uses UserContext, we need to mock it.
        mockUser = new User();
        mockUser.setId(1L); // Consistent with sampleCartDto.getUserId()
        mockUser.setUsername("testuser"); // This username should match @WithMockUser's username
        UserContext.setCurrentUser(mockUser); // Set up UserContext for tests
    }

    @AfterEach
    void tearDown() {
        UserContext.clear(); // Clean up UserContext
        // SecurityContextHolder.clearContext(); // Spring Security context usually cleared automatically
    }

    @Test
    @WithMockUser(username = "testuser") // Simulate an authenticated user, role doesn't matter due to isAuthenticated()
    void getCart_authenticatedUser_returnsCart() throws Exception {
        when(cartService.getCart(1L)).thenReturn(sampleCartDto);

        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    void getCart_unauthenticatedUser_returnsUnauthorized() throws Exception {
        UserContext.clear();
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(username = "testuser")
    void addItemToCart_authenticatedUser_returnsUpdatedCart() throws Exception {
        AddItemToCartRequest request = new AddItemToCartRequest();
        request.setProductId(101L);
        request.setQuantity(1);

        when(cartService.addItemToCart(1L, 101L, 1)).thenReturn(sampleCartDto);

        mockMvc.perform(post("/api/cart/items")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updateCartItem_authenticatedUser_returnsUpdatedCart() throws Exception {
        UpdateCartItemRequest request = new UpdateCartItemRequest();
        request.setQuantity(3);

        when(cartService.updateItemInCart(1L, 101L, 3)).thenReturn(sampleCartDto);

        mockMvc.perform(put("/api/cart/items/101")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    void removeItemFromCart_authenticatedUser_returnsUpdatedCart() throws Exception {
        when(cartService.removeItemFromCart(1L, 101L)).thenReturn(sampleCartDto);

        mockMvc.perform(delete("/api/cart/items/101")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L));
    }

    @Test
    @WithMockUser(username = "testuser")
    void clearCart_authenticatedUser_returnsNoContent() throws Exception {
        doNothing().when(cartService).clearCart(1L);

        mockMvc.perform(delete("/api/cart")
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
