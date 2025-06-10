package com.example.onlinestore.context;

import com.example.onlinestore.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserContextTest {

    @BeforeEach
    void setUp() {
        // Ensure the context is clear before each test
        UserContext.clear();
    }

    @AfterEach
    void tearDown() {
        // Ensure the context is clear after each test
        UserContext.clear();
    }

    @Test
    void setCurrentUser_shouldSetUserInThreadLocal() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        UserContext.setCurrentUser(user);
        User retrievedUser = UserContext.getCurrentUser();

        assertNotNull(retrievedUser);
        assertEquals(user.getId(), retrievedUser.getId());
        assertEquals(user.getUsername(), retrievedUser.getUsername());
    }

    @Test
    void getCurrentUser_whenNoUserSet_shouldReturnNull() {
        User retrievedUser = UserContext.getCurrentUser();
        assertNull(retrievedUser);
    }

    @Test
    void clear_shouldRemoveUserFromThreadLocal() {
        User user = new User();
        user.setId(1L);
        UserContext.setCurrentUser(user);

        assertNotNull(UserContext.getCurrentUser()); // Verify user is set

        UserContext.clear();
        assertNull(UserContext.getCurrentUser()); // Verify user is cleared
    }

    @Test
    void userContextIsolationBetweenThreads() throws InterruptedException {
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");

        // Thread 1 sets user1
        Thread thread1 = new Thread(() -> {
            UserContext.setCurrentUser(user1);
            assertEquals(user1.getId(), UserContext.getCurrentUser().getId());
            try {
                // Wait for thread 2 to set its user
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // Verify user1 is still set for thread 1
            assertEquals(user1.getId(), UserContext.getCurrentUser().getId());
        });

        // Thread 2 sets user2
        Thread thread2 = new Thread(() -> {
            try {
                // Wait a bit for thread 1 to set its user first
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            UserContext.setCurrentUser(user2);
            assertEquals(user2.getId(), UserContext.getCurrentUser().getId());
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        // Verify that the main thread still has no user set
        assertNull(UserContext.getCurrentUser());
    }
}
