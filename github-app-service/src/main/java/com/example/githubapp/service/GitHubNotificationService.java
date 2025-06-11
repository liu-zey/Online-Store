package com.example.githubapp.service;

import com.example.githubapp.dto.ProductData;

public interface GitHubNotificationService {
    void sendProductNotification(ProductData productData);
}
