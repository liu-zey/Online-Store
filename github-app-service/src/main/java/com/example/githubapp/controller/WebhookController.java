package com.example.githubapp.controller;

import com.example.githubapp.dto.ProductData;
import com.example.githubapp.service.GitHubNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookController.class);
    private final GitHubNotificationService notificationService;

    @Autowired
    public WebhookController(GitHubNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/new-product")
    public ResponseEntity<String> handleNewProductNotification(@RequestBody ProductData productData) {
        LOGGER.info("Received new product notification: {}", productData);
        try {
            notificationService.sendProductNotification(productData);
            return ResponseEntity.ok("Notification received and processed.");
        } catch (Exception e) {
            LOGGER.error("Error processing new product notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error processing notification: " + e.getMessage());
        }
    }
}
