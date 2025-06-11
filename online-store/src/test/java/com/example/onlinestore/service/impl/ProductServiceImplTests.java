package com.example.onlinestore.service.impl;

import com.example.onlinestore.dto.ProductCreateRequest;
import com.example.onlinestore.event.NewProductEvent;
import com.example.onlinestore.mapper.ProductMapper;
import com.example.onlinestore.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTests {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void addProduct_shouldSaveProductAndPublishEvent() {
        // Given
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("Test Product");
        request.setDescription("Test Description");
        request.setPrice(new BigDecimal("99.99"));

        ArgumentCaptor<Product> productArgumentCaptor = ArgumentCaptor.forClass(Product.class);
        ArgumentCaptor<NewProductEvent> eventArgumentCaptor = ArgumentCaptor.forClass(NewProductEvent.class);

        // When
        Product resultProduct = productService.addProduct(request);

        // Then
        // Verify productMapper.insert() was called
        verify(productMapper, times(1)).insert(productArgumentCaptor.capture());
        Product capturedProduct = productArgumentCaptor.getValue();

        assertNotNull(capturedProduct);
        assertEquals("Test Product", capturedProduct.getName());
        assertEquals("Test Description", capturedProduct.getDescription());
        assertEquals(new BigDecimal("99.99"), capturedProduct.getPrice());
        assertNotNull(capturedProduct.getCreatedAt()); // Should be set by the service

        // Verify applicationEventPublisher.publishEvent() was called
        verify(applicationEventPublisher, times(1)).publishEvent(eventArgumentCaptor.capture());
        NewProductEvent capturedEvent = eventArgumentCaptor.getValue();

        assertNotNull(capturedEvent);
        assertNotNull(capturedEvent.getProduct());
        assertSame(capturedProduct, capturedEvent.getProduct(), "The event should contain the same product instance that was saved.");

        // Verify the returned product is the same one that was processed
        assertNotNull(resultProduct);
        assertEquals("Test Product", resultProduct.getName());
        // The ID would be set by MyBatis via keyProperty, so it might be null here if not mocked
        // For this test, we primarily care that the product object processed is returned.
        // If ID generation is critical to test here, we'd need to mock doAnswer on productMapper.insert
        // e.g., doAnswer(invocation -> { Product p = invocation.getArgument(0); p.setId(1L); return null; }).when(productMapper).insert(any(Product.class));

        assertSame(capturedProduct, resultProduct, "The returned product should be the same instance that was saved and evented.");
    }
}
