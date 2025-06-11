package com.example.githubapp.service;

import com.example.githubapp.dto.ProductData;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct; // Ensure this import is correct for Spring Boot 3+
import java.io.IOException;
import java.util.Objects;

@Service
public class GitHubNotificationServiceImpl implements GitHubNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubNotificationServiceImpl.class);

    private long githubAppIdLong;
    private String privateKey;
    private long installationId;
    private String targetRepository;

    public GitHubNotificationServiceImpl() {
        // Constructor is now simpler, initialization moved to @PostConstruct
    }

    @PostConstruct
    public void init() {
        String githubAppIdEnv = System.getenv("GITHUB_APP_ID");
        String privateKeyEnv = System.getenv("GITHUB_APP_PRIVATE_KEY");
        String installationIdEnv = System.getenv("GITHUB_INSTALLATION_ID");
        String targetRepositoryEnv = System.getenv("GITHUB_TARGET_REPOSITORY");

        if (githubAppIdEnv == null || githubAppIdEnv.trim().isEmpty()) {
            throw new IllegalStateException("Environment variable GITHUB_APP_ID is not set or is empty.");
        }
        if (privateKeyEnv == null || privateKeyEnv.trim().isEmpty()) {
            throw new IllegalStateException("Environment variable GITHUB_APP_PRIVATE_KEY is not set or is empty.");
        }
        if (installationIdEnv == null || installationIdEnv.trim().isEmpty()) {
            throw new IllegalStateException("Environment variable GITHUB_INSTALLATION_ID is not set or is empty.");
        }
        if (targetRepositoryEnv == null || targetRepositoryEnv.trim().isEmpty()) {
            throw new IllegalStateException("Environment variable GITHUB_TARGET_REPOSITORY is not set or is empty.");
        }

        try {
            this.githubAppIdLong = Long.parseLong(githubAppIdEnv.trim());
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid format for GITHUB_APP_ID: '{}'. It must be a number.", githubAppIdEnv, e);
            throw new IllegalStateException("Invalid GITHUB_APP_ID format: " + githubAppIdEnv, e);
        }

        try {
            this.installationId = Long.parseLong(installationIdEnv.trim());
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid format for GITHUB_INSTALLATION_ID: '{}'. It must be a number.", installationIdEnv, e);
            throw new IllegalStateException("Invalid GITHUB_INSTALLATION_ID format: " + installationIdEnv, e);
        }

        // Replace literal \n with actual newlines if the environment variable contains them.
        // This is common if the key is copied and pasted as a single line with \n.
        this.privateKey = privateKeyEnv.replace("\\n", "\n");
        this.targetRepository = targetRepositoryEnv.trim();

        LOGGER.info("GitHubNotificationService initialized with configuration from environment variables.");
        LOGGER.debug("GITHUB_APP_ID (long): {}", this.githubAppIdLong);
        LOGGER.debug("GITHUB_INSTALLATION_ID (long): {}", this.installationId);
        LOGGER.debug("GITHUB_TARGET_REPOSITORY: {}", this.targetRepository);
        // Private key is sensitive, not logging its content.
    }

    @Override
    public void sendProductNotification(ProductData productData) {
        // Ensure service is initialized (fields are not null/0 due to PostConstruct checks)
        if (this.privateKey == null || this.targetRepository == null || this.githubAppIdLong == 0 || this.installationId == 0) {
             LOGGER.error("Service not properly initialized. Check environment variable configuration.");
             // This state should ideally be prevented by @PostConstruct checks.
             throw new IllegalStateException("Service not initialized. Missing GitHub configuration.");
        }

        LOGGER.info("Received request to send GitHub notification for product: {}", productData.getName());

        try {
            LOGGER.debug("Attempting to authenticate with GitHub App ID (long): {}", githubAppIdLong);
            LOGGER.debug("Using Installation ID: {}", installationId);
            LOGGER.debug("Target repository: {}", targetRepository);

            GitHub github = new GitHubBuilder()
                    .withAppInstallation(githubAppIdLong, installationId, privateKey)
                    .build();

            LOGGER.info("Successfully authenticated with GitHub. Rate limit: {}", github.getRateLimit());

            GHRepository repo = github.getRepository(targetRepository);
            LOGGER.info("Successfully obtained repository: {}", repo.getFullName());

            String title = "New Product Added: " + productData.getName();
            String body = "A new product has been added to the store:\n\n" +
                          "**Name:** " + productData.getName() + "\n" +
                          "**Description:** " + productData.getDescription() + "\n" +
                          "**Price:** $" + productData.getPrice().toString() + "\n" +
                          "**URL:** " + (productData.getProductUrl() != null ? productData.getProductUrl() : "N/A");

            org.kohsuke.github.GHIssue issue = repo.createIssue(title)
                    .body(body)
                    .create();

            LOGGER.info("Successfully created GitHub issue #{} in repository {}. URL: {}",
                    issue.getNumber(), repo.getFullName(), issue.getHtmlUrl());

        } catch (IOException e) {
            LOGGER.error("Failed to create GitHub issue for product: {}. Error: {}", productData.getName(), e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred while creating GitHub issue for product: {}. Error: {}", productData.getName(), e.getMessage(), e);
        }
    }
}
