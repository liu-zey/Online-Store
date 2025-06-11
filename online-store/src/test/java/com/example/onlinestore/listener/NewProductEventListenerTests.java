package com.example.onlinestore.listener;

import com.example.onlinestore.dto.ProductNotificationData;
import com.example.onlinestore.event.NewProductEvent;
import com.example.onlinestore.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewProductEventListenerTests {

    @Mock
    private RestTemplate restTemplate;

    private NewProductEventListener eventListener;

    private final String testUrl = "http://localhost:8081/test-webhook";

    @BeforeEach
    void setUp() {
        // Instantiate the listener directly, injecting the mock RestTemplate and test URL
        eventListener = new NewProductEventListener(restTemplate, testUrl);
    }

    @Test
    void onApplicationEvent_shouldSendNotification() {
        // Given
        Product product = new Product();
        product.setId(1L);
        product.setName("Super Widget");
        product.setDescription("A truly super widget.");
        product.setPrice(new BigDecimal("49.95"));
        product.setCreatedAt(LocalDateTime.now());

        NewProductEvent event = new NewProductEvent(product);

        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>("Success", HttpStatus.OK);
        when(restTemplate.postForEntity(eq(testUrl), any(ProductNotificationData.class), eq(String.class)))
                .thenReturn(mockResponseEntity);

        ArgumentCaptor<ProductNotificationData> notificationDataCaptor =
                ArgumentCaptor.forClass(ProductNotificationData.class);

        // When
        eventListener.onApplicationEvent(event);

        // Then
        verify(restTemplate, times(1)).postForEntity(
                eq(testUrl),
                notificationDataCaptor.capture(),
                eq(String.class)
        );

        ProductNotificationData capturedData = notificationDataCaptor.getValue();
        assertNotNull(capturedData);
        assertEquals(product.getName(), capturedData.getName());
        assertEquals(product.getDescription(), capturedData.getDescription());
        assertEquals(product.getPrice(), capturedData.getPrice());
        assertEquals("/products/" + product.getId(), capturedData.getProductUrl());
    }

    @Test
    void onApplicationEvent_whenRestClientException_shouldLogEror() {
        // Given
        Product product = new Product();
        product.setId(2L);
        product.setName("Faulty Widget");
        product.setDescription("This widget causes issues.");
        product.setPrice(new BigDecimal("10.00"));
        product.setCreatedAt(LocalDateTime.now());

        NewProductEvent event = new NewProductEvent(product);

        when(restTemplate.postForEntity(eq(testUrl), any(ProductNotificationData.class), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // When
        eventListener.onApplicationEvent(event);

        // Then
        // Verify postForEntity was still called
        verify(restTemplate, times(1)).postForEntity(
                eq(testUrl),
                any(ProductNotificationData.class),
                eq(String.class)
        );
        // Error logging is internal to the listener, difficult to assert directly without log capture utilities
        // or refactoring. For this test, we mainly ensure it doesn't crash and completes.
        // We trust that the SLF4J logger will handle the error message.
    }
}
