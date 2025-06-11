package com.example.onlinestore.listener;

import com.example.onlinestore.dto.ProductNotificationData;
import com.example.onlinestore.event.NewProductEvent;
import com.example.onlinestore.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class NewProductEventListener implements ApplicationListener<NewProductEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewProductEventListener.class);

    private final RestTemplate restTemplate;
    private final String githubAppServiceUrl;

    @Autowired
    public NewProductEventListener(RestTemplate restTemplate,
                                   @Value("${github.app.service.url}") String githubAppServiceUrl) {
        this.restTemplate = restTemplate;
        this.githubAppServiceUrl = githubAppServiceUrl;
    }

    @Override
    public void onApplicationEvent(NewProductEvent event) {
        Product product = event.getProduct();
        LOGGER.info("Received NewProductEvent for product ID: {}, Name: {}", product.getId(), product.getName());

        // Construct product URL (placeholder)
        // Assuming the store has a base URL and products are accessible via /products/{id}
        // This base URL could also come from configuration if needed.
        String productUrl = "/products/" + product.getId();

        ProductNotificationData notificationData = new ProductNotificationData(
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                productUrl
        );

        try {
            LOGGER.info("Sending product notification to {}: {}", githubAppServiceUrl, notificationData);
            restTemplate.postForEntity(githubAppServiceUrl, notificationData, String.class);
            LOGGER.info("Successfully sent product notification for product ID: {}", product.getId());
        } catch (RestClientException e) {
            LOGGER.error("Error sending product notification for product ID: {}. Error: {}", product.getId(), e.getMessage());
            // Depending on requirements, might add retry logic or dead-letter queue here.
        }
    }
}
