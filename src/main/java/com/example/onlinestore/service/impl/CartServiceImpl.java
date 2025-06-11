package com.example.onlinestore.service.impl;

import com.example.onlinestore.dto.CartDto;
import com.example.onlinestore.dto.CartItemDto;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.model.ProductImage; // For image URL in getCart
import com.example.onlinestore.service.CartService;
import com.example.onlinestore.service.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
// JavaTimeModule is usually registered globally, but explicit registration here ensures it for this ObjectMapper instance
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.Iterator; // For safe removal from list

@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);
    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_EXPIRATION_DAYS = 30;

    private final StringRedisTemplate redisTemplate;
    private final ProductService productService;
    private final ObjectMapper objectMapper; // For JSON serialization

    @Autowired
    public CartServiceImpl(StringRedisTemplate redisTemplate, ProductService productService, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.productService = productService;
        // It's generally better if ObjectMapper is configured globally with JavaTimeModule.
        // Making a copy and re-registering can lead to inconsistencies if not careful.
        // However, if global config is not assured, this is a way to ensure it for this service.
        this.objectMapper = objectMapper.copy();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    private String getCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private List<CartItemDto> getCartItemsFromRedis(Long userId) {
        String cartKey = getCartKey(userId);
        String jsonCart = redisTemplate.opsForValue().get(cartKey);
        if (jsonCart == null || jsonCart.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(jsonCart, new TypeReference<List<CartItemDto>>() {});
        } catch (IOException e) {
            log.error("Error deserializing cart for user {}: {}", userId, e.getMessage(), e);
            // Optionally delete corrupted cart data
            // redisTemplate.delete(cartKey);
            return new ArrayList<>(); // Return empty list on error
        }
    }

    private void saveCartItemsToRedis(Long userId, List<CartItemDto> items) {
        String cartKey = getCartKey(userId);
        try {
            String jsonCart = objectMapper.writeValueAsString(items);
            redisTemplate.opsForValue().set(cartKey, jsonCart, CART_EXPIRATION_DAYS, TimeUnit.DAYS);
        } catch (IOException e) {
            log.error("Error serializing cart for user {}: {}", userId, e.getMessage(), e);
            // Handle serialization error, maybe throw custom exception to indicate cart save failure
        }
    }

    @Override
    public CartDto getCart(Long userId) {
        Assert.notNull(userId, "User ID must not be null.");
        List<CartItemDto> items = getCartItemsFromRedis(userId);
        CartDto cartDto = new CartDto();
        cartDto.setUserId(userId);

        List<CartItemDto> validItems = new ArrayList<>();
        if (items != null) {
            for (Iterator<CartItemDto> iterator = items.iterator(); iterator.hasNext();) {
                CartItemDto item = iterator.next();
                Product product = productService.getProductById(item.getProductId());
                if (product != null && "ACTIVE".equals(product.getStatus())) {
                    item.setCurrentPrice(product.getPrice());
                    item.setStockQuantity(product.getStockQuantity());
                    item.setProductName(product.getName());
                     if (product.getImages() != null && !product.getImages().isEmpty()) {
                        item.setProductImageUrl(product.getImages().stream()
                                                      .filter(img -> img.isPrimary() || product.getImages().iterator().next() == img)
                                                      .findFirst()
                                                      .map(ProductImage::getImageUrl)
                                                      .orElse(product.getImages().iterator().next().getImageUrl()));
                    } else {
                        item.setProductImageUrl(null); // No image available
                    }
                    validItems.add(item);
                } else {
                    log.warn("Product ID {} in cart for user {} is no longer active or found. It will be removed from displayed cart.", item.getProductId(), userId);
                    // Item is not added to validItems, effectively removing it from the view but not yet from Redis.
                    // A more proactive approach might remove it from Redis here.
                }
            }
        }

        cartDto.setItems(validItems);
        cartDto.calculateGrandTotal();
        // Optional: If items were removed due to product status change, re-save the cleaned list to Redis
        // if (items != null && validItems.size() < items.size()) {
        //    saveCartItemsToRedis(userId, validItems); // This would persist the removal of unavailable items
        // }
        return cartDto;
    }

    @Override
    public CartDto addItemToCart(Long userId, Long productId, int quantity) {
        Assert.notNull(userId, "User ID must not be null.");
        Assert.notNull(productId, "Product ID must not be null.");
        Assert.isTrue(quantity > 0, "Quantity must be positive.");

        Product product = productService.getProductById(productId);
        if (product == null || !"ACTIVE".equals(product.getStatus())) {
            throw new IllegalArgumentException("Product not found or not available for purchase.");
        }
        if (product.getStockQuantity() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getName() +
                                               ". Available: " + product.getStockQuantity());
        }

        List<CartItemDto> items = getCartItemsFromRedis(userId);
        Optional<CartItemDto> existingItemOpt = items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItemDto existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;
            if (product.getStockQuantity() < newQuantity) {
                 throw new IllegalArgumentException("Insufficient stock to add more of product: " + product.getName() +
                                                   ". Requested total: " + newQuantity +
                                                   ", Available: " + product.getStockQuantity());
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setAddedAt(LocalDateTime.now());
        } else {
            CartItemDto newItem = new CartItemDto(product, quantity, product.getPrice());
            items.add(newItem);
        }

        saveCartItemsToRedis(userId, items);
        return getCart(userId);
    }

    @Override
    public CartDto updateItemInCart(Long userId, Long productId, int quantity) {
        Assert.notNull(userId, "User ID must not be null.");
        Assert.notNull(productId, "Product ID must not be null.");
        Assert.isTrue(quantity >= 0, "Quantity cannot be negative.");

        if (quantity == 0) {
            return removeItemFromCart(userId, productId);
        }

        List<CartItemDto> items = getCartItemsFromRedis(userId);
        Optional<CartItemDto> existingItemOpt = items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            Product product = productService.getProductById(productId);
            if (product == null || !"ACTIVE".equals(product.getStatus())) {
                items.removeIf(item -> item.getProductId().equals(productId)); // Remove if product became unavailable
                saveCartItemsToRedis(userId, items);
                log.warn("Product ID {} was removed from cart for user {} as it's no longer active or found.", productId, userId);
                throw new IllegalArgumentException("Product not found or not available for purchase, removed from cart.");
            }
            if (product.getStockQuantity() < quantity) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName() +
                                                   ". Requested: " + quantity +
                                                   ", Available: " + product.getStockQuantity());
            }
            CartItemDto existingItem = existingItemOpt.get();
            existingItem.setQuantity(quantity);
            existingItem.setAddedAt(LocalDateTime.now());
            saveCartItemsToRedis(userId, items);
        } else {
            throw new IllegalArgumentException("Item not found in cart to update.");
        }
        return getCart(userId);
    }

    @Override
    public CartDto removeItemFromCart(Long userId, Long productId) {
        Assert.notNull(userId, "User ID must not be null.");
        Assert.notNull(productId, "Product ID must not be null.");

        List<CartItemDto> items = getCartItemsFromRedis(userId);
        boolean removed = items.removeIf(item -> item.getProductId().equals(productId));

        if (removed) {
            saveCartItemsToRedis(userId, items);
        } else {
            log.warn("Attempted to remove product ID {} from cart of user {}, but item was not found.", productId, userId);
        }
        return getCart(userId);
    }

    @Override
    public void clearCart(Long userId) {
        Assert.notNull(userId, "User ID must not be null.");
        redisTemplate.delete(getCartKey(userId));
        log.info("Cleared cart for user id: {}", userId);
    }
}
