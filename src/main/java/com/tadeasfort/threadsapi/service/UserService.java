package com.tadeasfort.threadsapi.service;

import com.tadeasfort.threadsapi.entity.User;
import com.tadeasfort.threadsapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ThreadsApiClient threadsApiClient;

    public UserService(UserRepository userRepository, ThreadsApiClient threadsApiClient) {
        this.userRepository = userRepository;
        this.threadsApiClient = threadsApiClient;
    }

    /**
     * Create or update user from Threads profile data
     */
    public User createOrUpdateUser(String accessToken) {
        try {
            // Get user profile from Threads API
            Map<String, Object> profile = threadsApiClient.getUserProfile(accessToken);

            String threadsUserId = (String) profile.get("id");
            String username = (String) profile.get("username");
            String name = (String) profile.get("name");

            // Check if user already exists
            Optional<User> existingUser = userRepository.findByThreadsUserId(threadsUserId);

            User user;
            if (existingUser.isPresent()) {
                // Update existing user
                user = existingUser.get();
                user.setUsername(username);
                user.setName(name);
                user.setAccessToken(accessToken);
            } else {
                // Create new user
                user = new User(threadsUserId, username, name);
                user.setAccessToken(accessToken);
            }

            return userRepository.save(user);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create or update user: " + e.getMessage(), e);
        }
    }

    /**
     * Find user by Threads user ID
     */
    @Transactional(readOnly = true)
    public Optional<User> findByThreadsUserId(String threadsUserId) {
        return userRepository.findByThreadsUserId(threadsUserId);
    }

    /**
     * Find user by username
     */
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Update user's access token
     */
    public User updateAccessToken(String threadsUserId, String accessToken) {
        User user = userRepository.findByThreadsUserId(threadsUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + threadsUserId));

        user.setAccessToken(accessToken);
        return userRepository.save(user);
    }

    /**
     * Get user profile with fresh data from Threads API
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserProfileWithApiData(String threadsUserId) {
        User user = userRepository.findByThreadsUserId(threadsUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + threadsUserId));

        try {
            return threadsApiClient.getUserProfile(user.getAccessToken());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get user profile from API: " + e.getMessage(), e);
        }
    }
}