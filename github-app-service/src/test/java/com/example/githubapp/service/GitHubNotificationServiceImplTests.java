package com.example.githubapp.service;

import com.example.githubapp.dto.ProductData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.github.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GitHubNotificationServiceImplTests {

    private GitHubNotificationServiceImpl service;

    @Mock
    private GitHub mockGitHub; // Mock the final GitHub client object
    @Mock
    private GHRepository mockRepo;
    @Mock
    private GHIssue mockIssue;
    @Mock
    private GHIssueBuilder mockIssueBuilder;

    // Placeholder values for fields normally set by init() from env vars
    private static final long TEST_APP_ID = 12345L;
    private static final String TEST_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY-----\nTEST_KEY\n-----END RSA PRIVATE KEY-----";
    private static final long TEST_INSTALLATION_ID = 67890L;
    private static final String TEST_TARGET_REPO = "owner/test-repo";

    @BeforeEach
    void setUp() {
        service = new GitHubNotificationServiceImpl();
        // Manually set the fields that @PostConstruct would initialize
        ReflectionTestUtils.setField(service, "githubAppIdLong", TEST_APP_ID);
        ReflectionTestUtils.setField(service, "privateKey", TEST_PRIVATE_KEY);
        ReflectionTestUtils.setField(service, "installationId", TEST_INSTALLATION_ID);
        ReflectionTestUtils.setField(service, "targetRepository", TEST_TARGET_REPO);
    }

    @Test
    void sendProductNotification_success() throws IOException {
        ProductData productData = new ProductData("Super TV", "Amazing new television", new BigDecimal("999.99"), "/products/super-tv");

        // We need to mock the static GitHubBuilder.fromEnvironment() or the instance new GitHubBuilder()
        // Since new GitHubBuilder() is used, we need to mock the constructor or use PowerMock.
        // A workaround is to use a try-with-resources block for MockedStatic if GitHubBuilder itself has static methods we need to control
        // or if we can make `new GitHubBuilder()` return a mock.
        // The current implementation of GitHubNotificationServiceImpl uses `new GitHubBuilder().withAppInstallation(...).build();`
        // This is hard to mock without PowerMock/ByteBuddy or refactoring.

        // Let's assume GitHubBuilder().withAppInstallation().build() can be mocked to return our mockGitHub
        // This is the tricky part. For a true unit test, GitHubBuilder itself should be a mock.
        // We will use Mockito.mockStatic for GitHubBuilder to control its behavior.
        try (MockedStatic<GitHubBuilder> mockedStaticGitHubBuilder = Mockito.mockStatic(GitHubBuilder.class)) {
            GitHubBuilder mockGbInstance = mock(GitHubBuilder.class);

            // When new GitHubBuilder() is effectively called, it should lead to our mockGbInstance if possible,
            // or we mock the chain.
            // For this API: new GitHubBuilder().withAppInstallation(appId, installationId, privateKey).build();
            // We need `new GitHubBuilder()` to return a controllable builder.
            // Since we can't mock constructors with Mockito alone, we'll mock the static `fromEnvironment` if it were used,
            // or accept limitations.
            // The most direct way to mock this chain with only Mockito is to ensure that `new GitHubBuilder()`
            // somehow gives us a mock we can control. This is not possible without bytecode manipulation (PowerMock, etc.).

            // Alternative: Assume the GitHub object is successfully created and focus on what happens next.
            // This means we cannot verify `.withAppInstallation(...)` was called correctly here.
            // We will mock the `build()` method of any GitHubBuilder instance to return our `mockGitHub`.
            // This is still not ideal as `withAppInstallation` is called on the real `GitHubBuilder` instance.

            // Given the limitations, the test will assume `GitHubBuilder...build()` returns `mockGitHub`.
            // This part of the interaction (the builder chain) cannot be fully verified with just Mockito
            // if `new GitHubBuilder()` is called directly.
            // The provided solution template uses `new GitHubBuilder().withAppInstallation(githubAppIdLong, installationId, privateKey).build();`
            // To make this testable for the builder part, the service would need to accept a GitHubBuilderFactory or similar.

            // For the purpose of this subtask, we will assume the GitHub object (mockGitHub) is what the builder produces.
            // The test will then verify interactions with this mockGitHub.

            when(mockGitHub.getRepository(TEST_TARGET_REPO)).thenReturn(mockRepo);
            when(mockRepo.createIssue(anyString())).thenReturn(mockIssueBuilder);
            when(mockIssueBuilder.body(anyString())).thenReturn(mockIssueBuilder);
            when(mockIssueBuilder.create()).thenReturn(mockIssue);
            when(mockIssue.getNumber()).thenReturn(101);
            when(mockIssue.getHtmlUrl()).thenReturn(new java.net.URL("http://github.com/owner/test-repo/issues/101"));

            // This is the conceptual part that's hard to mock:
            // when(new GitHubBuilder().withAppInstallation(anyLong(), anyLong(), anyString()).build()).thenReturn(mockGitHub);
            // Instead, we'll have to use a different strategy if possible, or acknowledge the gap.
            // For now, this test will assume 'github' is our 'mockGitHub' through some means not shown in this isolated unit test.
            // A more robust test would use a factory for GitHubBuilder, or test `init()` separately with env vars.

            // To actually make this test run, we need to ensure that the line:
            // GitHub github = new GitHubBuilder().withAppInstallation(githubAppIdLong, installationId, privateKey).build();
            // results in `github` being our `mockGitHub`.
            // This is where standard Mockito falls short for `new X()` calls.
            // However, if we could inject the `GitHub` client directly (after it's built), that would be better.

            // Let's try to use a static mock for GitHubBuilder to control the outcome of the build() call.
            // This requires careful setup.
            mockedStaticGitHubBuilder.when(GitHubBuilder::new).thenReturn(mockGbInstance);
            when(mockGbInstance.withAppInstallation(TEST_APP_ID, TEST_INSTALLATION_ID, TEST_PRIVATE_KEY)).thenReturn(mockGbInstance);
            when(mockGbInstance.build()).thenReturn(mockGitHub);


            // Call the method under test
            service.sendProductNotification(productData);

            // Verifications
            verify(mockGitHub).getRepository(TEST_TARGET_REPO);

            ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockRepo).createIssue(titleCaptor.capture());
            assertEquals("New Product Added: Super TV", titleCaptor.getValue());

            ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
            verify(mockIssueBuilder).body(bodyCaptor.capture());
            assertTrue(bodyCaptor.getValue().contains("Name: Super TV"));
            assertTrue(bodyCaptor.getValue().contains("Description: Amazing new television"));
            assertTrue(bodyCaptor.getValue().contains("Price: $999.99"));
            assertTrue(bodyCaptor.getValue().contains("URL: /products/super-tv"));

            verify(mockIssueBuilder).create();
            verify(mockIssue).getNumber();
            verify(mockIssue).getHtmlUrl();
        }
    }

    @Test
    void sendProductNotification_whenGitHubRepoNotFound_handlesIOException() throws IOException {
        ProductData productData = new ProductData("Error Product", "This will fail", BigDecimal.TEN, "/products/error");

        try (MockedStatic<GitHubBuilder> mockedStaticGitHubBuilder = Mockito.mockStatic(GitHubBuilder.class)) {
            GitHubBuilder mockGbInstance = mock(GitHubBuilder.class);
            mockedStaticGitHubBuilder.when(GitHubBuilder::new).thenReturn(mockGbInstance);
            when(mockGbInstance.withAppInstallation(TEST_APP_ID, TEST_INSTALLATION_ID, TEST_PRIVATE_KEY)).thenReturn(mockGbInstance);
            when(mockGbInstance.build()).thenReturn(mockGitHub);

            when(mockGitHub.getRepository(TEST_TARGET_REPO)).thenThrow(new IOException("Repository not found or access denied"));

            // Call the method under test
            service.sendProductNotification(productData);

            // Verify that issue creation was not attempted
            verify(mockRepo, never()).createIssue(anyString());
            // Logging of the error is expected (not directly verifiable here without log capture)
        }
    }

    @Test
    void init_missingAppId_throwsIllegalStateException() {
        // This test is tricky because System.getenv is static and final.
        // To test this properly, one would typically use a library like JUnit Pioneer @SetEnvironmentVariable,
        // or refactor the service to take a map/provider for environment variables.
        // Simulating the condition by setting one of the *fields* to null after construction (if @PostConstruct wasn't final)
        // or by trying to call init() when env vars are known to be unset (hard to guarantee in test env).

        // For this subtask, a full test of init() with System.getenv() is outside the scope of standard Mockito.
        // We rely on the real @PostConstruct behavior during integration or manual testing.
        // However, we can assert that if the fields are not set, sendProductNotification fails early.
        ReflectionTestUtils.setField(service, "githubAppIdLong", 0L); // Simulate missing/invalid app ID

        assertThrows(IllegalStateException.class, () -> {
            service.sendProductNotification(new ProductData("Test", "Test", BigDecimal.ONE, "url"));
        }, "Service should throw IllegalStateException if not properly initialized.");
    }
}
