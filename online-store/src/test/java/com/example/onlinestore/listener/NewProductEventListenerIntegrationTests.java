package com.example.onlinestore.listener;

import com.example.onlinestore.dto.ProductNotificationData;
import com.example.onlinestore.event.NewProductEvent;
import com.example.onlinestore.model.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class NewProductEventListenerIntegrationTests {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ObjectMapper objectMapper; // For deserializing the request body

    private WireMockServer wireMockServer;

    // This method will override the property from application.yml
    @DynamicPropertySource
    static void overrideGithubAppServiceUrl(DynamicPropertyRegistry registry) {
        // The port will be set dynamically in setup()
        // Here we just define the property name. The value will be set once wiremock starts.
        // So we need a way to update this. A common pattern is to use a fixed port for tests
        // or use a placeholder that gets updated.
        // For simplicity, let's assume we'll update the listener's URL directly or use a known port for wiremock.
        // A cleaner way is to have the test listener pick up the dynamic port.
        // Let's configure WireMock with a fixed port for easier @DynamicPropertySource usage.
        // Or, we can set the property after wireMockServer is started in @BeforeEach and refresh context / re-inject listener.
        // The simplest for now: use a placeholder and update it in the test.
        // However, @DynamicPropertySource is static, so it's tricky.

        // Let's use a fixed port for WireMock in this test for simplicity with @DynamicPropertySource
        // Alternative: manually create listener with dynamic URL if @DynamicPropertySource is hard.
        // For this test, let's assume the listener will pick up the URL set by @DynamicPropertySource.
        // The WireMock server needs to be started before this source is used.
        // This points to using @RegisterExtension for WireMock or a static WireMockServer instance if using dynamic ports.

        // Using a fixed port for simplicity.
        // If using dynamicPort for WireMockServer, this would need to be more complex,
        // possibly by not using @Autowired for NewProductEventListener and instantiating it manually with the dynamic URL.
        // Or by using TestPropertyValues.
    }

    @BeforeEach
    void setup() {
        // Start WireMock server on a dynamic port
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Override the property for the event listener to use the WireMock server's URL
        // This is the tricky part with @DynamicPropertySource being static.
        // A common solution is to use TestPropertySource with a placeholder that we can control,
        // or to re-initialize the listener bean with the new URL.
        // For this test, we'll rely on the listener picking up the URL if it were refreshed,
        // or more simply, ensure our test setup makes the listener use this URL.
        // The @DynamicPropertySource is better used with @RegisterExtension for WireMock.

        // Let's adjust the strategy: We will set the system property which @DynamicPropertySource can pick up.
        System.setProperty("github.app.service.url.test", wireMockServer.baseUrl() + "/webhook/new-product");
    }

    @DynamicPropertySource // This will be used to set the property for the Spring context
    static void properties(DynamicPropertyRegistry registry) {
        // This relies on the system property being set in @BeforeEach.
        // Note: This is a bit of a workaround due to JUnit5's lifecycle.
        // A cleaner approach might involve @Testcontainers or more complex Spring test configurations.
        String apiUrl = System.getProperty("github.app.service.url.test");
        if (apiUrl != null) {
            registry.add("github.app.service.url", () -> apiUrl);
        } else {
            // Fallback or fail if property not set, though @BeforeEach should set it.
            // For safety, could use a default or fixed port if system property not found.
             registry.add("github.app.service.url", () -> "http://localhost:8090/webhook/new-product"); // Default if not set
        }
    }


    @AfterEach
    void teardown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
        System.clearProperty("github.app.service.url.test");
    }

    @Test
    void testOnApplicationEvent_sendsCorrectRequest() throws JsonProcessingException {
        // Arrange
        String expectedPath = "/webhook/new-product";
        // Re-configure WireMock stubs using the actual base URL from the running server
        WireMock.configureFor("localhost", wireMockServer.port());
        stubFor(post(urlEqualTo(expectedPath))
                .willReturn(aResponse().withStatus(200).withBody("Mock response")));

        Product product = new Product();
        product.setId(123L);
        product.setName("Integration Test Product");
        product.setDescription("A product for integration testing the listener.");
        product.setPrice(new BigDecimal("199.99"));
        product.setCreatedAt(LocalDateTime.now());

        NewProductEvent event = new NewProductEvent(product);

        // Act
        applicationEventPublisher.publishEvent(event);

        // Assert
        // Allow some time for the event to be processed asynchronously if that's the case
        // (ApplicationListener is typically synchronous unless event publishing is async)
        try {
            Thread.sleep(500); // Small delay just in case, though usually not needed for sync listeners
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }


        verify(1, postRequestedFor(urlEqualTo(expectedPath))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.name", equalTo(product.getName())))
                .withRequestBody(matchingJsonPath("$.description", equalTo(product.getDescription())))
                .withRequestBody(matchingJsonPath("$.price", equalTo(product.getPrice().toString()))) // Compare as string
                .withRequestBody(matchingJsonPath("$.productUrl", equalTo("/products/" + product.getId()))));

        // Optionally, capture and deserialize the body for more detailed assertions
        var MOCK_REQUEST = wireMockServer.getAllServeEvents().get(0).getRequest();
        String requestBody = MOCK_REQUEST.getBodyAsString();
        ProductNotificationData notificationData = objectMapper.readValue(requestBody, ProductNotificationData.class);

        assertEquals(product.getName(), notificationData.getName());
        assertEquals(product.getDescription(), notificationData.getDescription());
        assertEquals(0, product.getPrice().compareTo(notificationData.getPrice())); // BigDecimal comparison
        assertEquals("/products/" + product.getId(), notificationData.getProductUrl());
    }
}
