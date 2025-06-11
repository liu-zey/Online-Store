package com.example.onlinestore.controller;

import com.example.onlinestore.context.UserContext; // To get current user ID
import com.example.onlinestore.dto.CartDto;
import com.example.onlinestore.dto.AddItemToCartRequest;
import com.example.onlinestore.dto.UpdateCartItemRequest;
import com.example.onlinestore.exception.UnauthorizedException;
import com.example.onlinestore.model.User;
import com.example.onlinestore.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication; // For Spring Security context
import org.springframework.security.core.context.SecurityContextHolder; // For Spring Security context
import org.springframework.security.core.userdetails.UserDetails; // For Spring Security context
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("isAuthenticated()") // All methods require an authenticated user
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    private Long getCurrentUserId() {
        // Prefer fetching user details from SecurityContextHolder if Spring Security is fully integrated
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            // This assumes your UserDetails principal's username is the key to get your custom User model's ID
            // Or, if your UserDetails IS your custom User model, you can cast directly.
            // For now, let's assume UserContext is still populated by AuthInterceptor and is reliable.
            // If AuthInterceptor correctly populates SecurityContextHolder, this can be simplified.
        }

        User currentUser = UserContext.getCurrentUser(); // Still relying on UserContext as per current design
        if (currentUser == null || currentUser.getId() == null) {
            // This should ideally be caught by Spring Security's authentication mechanism
            // before reaching here if @PreAuthorize("isAuthenticated()") is effective.
            // Throwing an exception here is a fallback.
            throw new UnauthorizedException("User not authenticated or user ID not available in UserContext.");
        }
        return currentUser.getId();
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart() {
        Long userId = getCurrentUserId();
        CartDto cart = cartService.getCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItemToCart(@Valid @RequestBody AddItemToCartRequest request) {
        Long userId = getCurrentUserId();
        CartDto cart = cartService.addItemToCart(userId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(cart);
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartDto> updateCartItem(@PathVariable Long productId,
                                                  @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = getCurrentUserId();
        CartDto cart = cartService.updateItemInCart(userId, productId, request.getQuantity());
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartDto> removeItemFromCart(@PathVariable Long productId) {
        Long userId = getCurrentUserId();
        CartDto cart = cartService.removeItemFromCart(userId, productId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart() {
        Long userId = getCurrentUserId();
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
