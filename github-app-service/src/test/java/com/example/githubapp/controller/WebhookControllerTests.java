package com.example.githubapp.controller;

import com.example.githubapp.dto.ProductData;
import com.example.githubapp.service.GitHubNotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
public class WebhookControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitHubNotificationService gitHubNotificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void receiveNewProductWebhook_whenValidData_shouldCallServiceAndReturnOk() throws Exception {
        // Given
        ProductData productData = new ProductData("Test Product", "Description", new BigDecimal("9.99"), "/product/1");

        // When
        ResultActions resultActions = mockMvc.perform(post("/webhook/new-product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productData)));

        // Then
        verify(gitHubNotificationService, times(1)).sendProductNotification(any(ProductData.class));
        resultActions.andExpect(status().isOk())
                .andExpect(content().string("Notification received and processed."));
    }

    @Test
    void receiveNewProductWebhook_whenServiceThrowsException_shouldReturnInternalServerError() throws Exception {
        // Given
        ProductData productData = new ProductData("Test Product", "Description", new BigDecimal("9.99"), "/product/1");
        doThrow(new RuntimeException("Service failure")).when(gitHubNotificationService).sendProductNotification(any(ProductData.class));

        // When
        ResultActions resultActions = mockMvc.perform(post("/webhook/new-product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productData)));

        // Then
        verify(gitHubNotificationService, times(1)).sendProductNotification(any(ProductData.class));
        resultActions.andExpect(status().isInternalServerError())
                .andExpect(content().string("Error processing notification: Service failure"));
    }

    @Test
    void receiveNewProductWebhook_whenMalformedJson_shouldReturnBadRequest() throws Exception {
        // Given
        String malformedJson = "{\"name\":\"Test Product\", \"description\":\"Description\", \"price\":\"invalid-price\"}";

        // When
        ResultActions resultActions = mockMvc.perform(post("/webhook/new-product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson));

        // Then
        // Spring Boot's default error handling for deserialization errors should result in a 400 Bad Request.
        // The service method should not be called.
        verify(gitHubNotificationService, times(0)).sendProductNotification(any(ProductData.class));
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    void receiveNewProductWebhook_whenEmptyRequestBody_shouldReturnBadRequest() throws Exception {
        // Given an empty request body
        // When
        ResultActions resultActions = mockMvc.perform(post("/webhook/new-product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""));

        // Then
        verify(gitHubNotificationService, times(0)).sendProductNotification(any(ProductData.class));
        resultActions.andExpect(status().isBadRequest());
    }
}
