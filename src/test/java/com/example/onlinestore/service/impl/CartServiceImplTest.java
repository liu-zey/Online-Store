package com.example.onlinestore.service.impl;

import com.example.onlinestore.dto.CartDto;
import com.example.onlinestore.dto.CartItemDto;
import com.example.onlinestore.model.Product;
import com.example.onlinestore.model.ProductImage;
import com.example.onlinestore.service.ProductService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ProductService productService;
    @Mock
    private ValueOperations<String, String> valueOperations; // Mock for redis ops

    @Spy // Use @Spy for ObjectMapper to allow real method calls but also verify interactions if needed
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private CartServiceImpl cartService;

    private Long userId = 1L;
    private String cartKey = "cart:1";
    private Product sampleProduct1;
    private Product sampleProduct2;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        sampleProduct1 = new Product();
        sampleProduct1.setId(101L);
        sampleProduct1.setName("Laptop X");
        sampleProduct1.setPrice(new BigDecimal("1200.00"));
        sampleProduct1.setStockQuantity(10);
        sampleProduct1.setStatus("ACTIVE");
        ProductImage img1 = new ProductImage();
        img1.setImageUrl("http://example.com/laptop.jpg");
        img1.setPrimary(true); // Ensure one is primary for consistent test
        Set<ProductImage> images1 = new HashSet<>();
        images1.add(img1);
        sampleProduct1.setImages(images1);


        sampleProduct2 = new Product();
        sampleProduct2.setId(102L);
        sampleProduct2.setName("Mouse Y");
        sampleProduct2.setPrice(new BigDecimal("25.00"));
        sampleProduct2.setStockQuantity(50);
        sampleProduct2.setStatus("ACTIVE");
    }

    private String toJson(List<CartItemDto> items) throws IOException {
        return objectMapper.writeValueAsString(items);
    }

    @Test
    void getCart_emptyCart_returnsEmptyCartDto() throws IOException {
        when(valueOperations.get(cartKey)).thenReturn(null); // No cart in Redis

        CartDto cart = cartService.getCart(userId);

        assertNotNull(cart);
        assertEquals(userId, cart.getUserId());
        assertTrue(cart.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, cart.getGrandTotal());
    }

    @Test
    void getCart_withItems_returnsCartDtoWithRefreshedDetails() throws IOException {
        CartItemDto itemInRedis = new CartItemDto();
        itemInRedis.setProductId(sampleProduct1.getId());
        itemInRedis.setQuantity(1);
        itemInRedis.setPriceAtAddition(new BigDecimal("1190.00"));
        itemInRedis.setAddedAt(LocalDateTime.now().minusDays(1));

        List<CartItemDto> redisItems = Collections.singletonList(itemInRedis);
        when(valueOperations.get(cartKey)).thenReturn(toJson(redisItems));
        when(productService.getProductById(sampleProduct1.getId())).thenReturn(sampleProduct1);

        CartDto cart = cartService.getCart(userId);

        assertNotNull(cart);
        assertEquals(1, cart.getItems().size());
        CartItemDto displayedItem = cart.getItems().get(0);
        assertEquals(sampleProduct1.getName(), displayedItem.getProductName());
        assertEquals(sampleProduct1.getPrice(), displayedItem.getCurrentPrice());
        assertEquals(itemInRedis.getPriceAtAddition(), displayedItem.getPriceAtAddition());
        assertEquals(sampleProduct1.getStockQuantity(), displayedItem.getStockQuantity());
        assertEquals(new BigDecimal("1190.00"), cart.getGrandTotal());
    }

    @Test
    void getCart_productNoLongerActive_itemRemovedFromDisplayedCart() throws IOException {
        CartItemDto itemInRedis = new CartItemDto();
        itemInRedis.setProductId(sampleProduct1.getId());
        itemInRedis.setQuantity(1);
        itemInRedis.setPriceAtAddition(new BigDecimal("1200.00"));

        List<CartItemDto> redisItems = Collections.singletonList(itemInRedis);
        when(valueOperations.get(cartKey)).thenReturn(toJson(redisItems));

        Product inactiveProduct = new Product();
        inactiveProduct.setId(sampleProduct1.getId());
        inactiveProduct.setStatus("INACTIVE");
        inactiveProduct.setPrice(new BigDecimal("1200.00"));
        inactiveProduct.setStockQuantity(5);
        when(productService.getProductById(sampleProduct1.getId())).thenReturn(inactiveProduct);

        CartDto cart = cartService.getCart(userId);

        assertNotNull(cart);
        // Item should be filtered out from the 'validItems' list in getCart()
        assertTrue(cart.getItems().isEmpty(), "Item for inactive product should be removed from displayed cart");
        assertEquals(BigDecimal.ZERO, cart.getGrandTotal(), "Grand total should be zero if all items removed");
    }


    @Test
    void addItemToCart_newItem_success() throws IOException {
        when(valueOperations.get(cartKey)).thenReturn(toJson(new ArrayList<>()));
        when(productService.getProductById(sampleProduct1.getId())).thenReturn(sampleProduct1);

        CartDto cart = cartService.addItemToCart(userId, sampleProduct1.getId(), 2);

        assertNotNull(cart);
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(cartKey), captor.capture(), eq(30L), eq(TimeUnit.DAYS));
        List<CartItemDto> savedItems = objectMapper.readValue(captor.getValue(), new TypeReference<List<CartItemDto>>() {});
        assertEquals(1, savedItems.size());
        assertEquals(sampleProduct1.getId(), savedItems.get(0).getProductId());
    }

    @Test
    void addItemToCart_increaseQuantity_success() throws IOException {
        CartItemDto existingItem = new CartItemDto(sampleProduct1, 1, sampleProduct1.getPrice());
        List<CartItemDto> initialItems = new ArrayList<>(Collections.singletonList(existingItem));
        when(valueOperations.get(cartKey)).thenReturn(toJson(initialItems));
        when(productService.getProductById(sampleProduct1.getId())).thenReturn(sampleProduct1);

        CartDto cart = cartService.addItemToCart(userId, sampleProduct1.getId(), 1);

        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
    }

    @Test
    void addItemToCart_insufficientStock_throwsException() {
        when(productService.getProductById(sampleProduct1.getId())).thenReturn(sampleProduct1);
        sampleProduct1.setStockQuantity(1);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            cartService.addItemToCart(userId, sampleProduct1.getId(), 2);
        });
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    void updateItemInCart_success() throws IOException {
        CartItemDto existingItem = new CartItemDto(sampleProduct1, 1, sampleProduct1.getPrice());
        List<CartItemDto> initialItems = new ArrayList<>(Collections.singletonList(existingItem));
        when(valueOperations.get(cartKey)).thenReturn(toJson(initialItems));
        when(productService.getProductById(sampleProduct1.getId())).thenReturn(sampleProduct1);

        CartDto cart = cartService.updateItemInCart(userId, sampleProduct1.getId(), 5);

        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
    }

    @Test
    void updateItemInCart_quantityZero_removesItem() throws IOException {
        CartItemDto item1 = new CartItemDto(sampleProduct1, 2, sampleProduct1.getPrice());
        CartItemDto item2 = new CartItemDto(sampleProduct2, 1, sampleProduct2.getPrice());
        List<CartItemDto> initialItems = new ArrayList<>(Arrays.asList(item1, item2));

        when(valueOperations.get(cartKey)).thenReturn(toJson(initialItems));
        // Product service mock for item2 which remains in cart after item1 is removed by qty 0 update
        when(productService.getProductById(sampleProduct2.getId())).thenReturn(sampleProduct2);

        CartDto cart = cartService.updateItemInCart(userId, sampleProduct1.getId(), 0);

        assertEquals(1, cart.getItems().size());
        assertEquals(sampleProduct2.getId(), cart.getItems().get(0).getProductId());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(cartKey), captor.capture(), eq(30L), eq(TimeUnit.DAYS));
        List<CartItemDto> savedItems = objectMapper.readValue(captor.getValue(), new TypeReference<List<CartItemDto>>() {});
        assertEquals(1, savedItems.size());
        assertEquals(sampleProduct2.getId(), savedItems.get(0).getProductId());
    }


    @Test
    void removeItemFromCart_success() throws IOException {
        CartItemDto item1 = new CartItemDto(sampleProduct1, 1, sampleProduct1.getPrice());
        CartItemDto item2 = new CartItemDto(sampleProduct2, 1, sampleProduct2.getPrice());
        List<CartItemDto> initialItems = new ArrayList<>(Arrays.asList(item1, item2));
        when(valueOperations.get(cartKey)).thenReturn(toJson(initialItems));
        // Product service mock for item2 which remains in cart
        when(productService.getProductById(sampleProduct2.getId())).thenReturn(sampleProduct2);


        CartDto cart = cartService.removeItemFromCart(userId, sampleProduct1.getId());

        assertEquals(1, cart.getItems().size());
        assertEquals(sampleProduct2.getId(), cart.getItems().get(0).getProductId());
    }

    @Test
    void clearCart_success() {
        cartService.clearCart(userId);
        verify(redisTemplate).delete(cartKey);
    }
}
