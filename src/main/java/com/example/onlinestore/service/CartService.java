package com.example.onlinestore.service;

import com.example.onlinestore.dto.CartDto;
// import com.example.onlinestore.dto.CartItemDto; // Not directly exposed if CartDto is comprehensive

public interface CartService {
    CartDto getCart(Long userId);
    CartDto addItemToCart(Long userId, Long productId, int quantity);
    CartDto updateItemInCart(Long userId, Long productId, int quantity);
    CartDto removeItemFromCart(Long userId, Long productId);
    void clearCart(Long userId);
}
