package com.tadeasfort.threadsapi.service;

import com.tadeasfort.threadsapi.dto.WebhookRequest;
import com.tadeasfort.threadsapi.entity.User;
import com.tadeasfort.threadsapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);

    private final UserRepository userRepository;

    public WebhookService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Handle user deauthorization (uninstall) event
     * This is called when a user revokes permissions for your app
     */
    public void handleUserDeauthorization(WebhookRequest webhookRequest) {
        logger.info("Processing user deauthorization webhook");

        try {
            for (WebhookRequest.WebhookEntry entry : webhookRequest.getEntries()) {
                String userId = entry.getId();

                logger.info("Processing deauthorization for user: {}", userId);

                // Find user by Threads user ID
                Optional<User> userOptional = userRepository.findByThreadsUserId(userId);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    // Invalidate access token and mark as deauthorized
                    user.setAccessToken(null);
                    user.setIsActive(false);

                    userRepository.save(user);

                    logger.info("Successfully deauthorized user: {} ({})", user.getUsername(), userId);
                } else {
                    logger.warn("User not found for deauthorization: {}", userId);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing user deauthorization webhook", e);
            throw new RuntimeException("Failed to process deauthorization webhook", e);
        }
    }

    /**
     * Handle user deletion event
     * This is called when a user deletes their Meta account
     */
    public void handleUserDeletion(WebhookRequest webhookRequest) {
        logger.info("Processing user deletion webhook");

        try {
            for (WebhookRequest.WebhookEntry entry : webhookRequest.getEntries()) {
                String userId = entry.getId();

                logger.info("Processing deletion for user: {}", userId);

                // Find user by Threads user ID
                Optional<User> userOptional = userRepository.findByThreadsUserId(userId);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();

                    // Option 1: Soft delete - mark as deleted but keep data for audit
                    user.setAccessToken(null);
                    user.setIsActive(false);
                    user.setIsDeleted(true);

                    userRepository.save(user);

                    // Option 2: Hard delete - completely remove user and related data
                    // Uncomment the following lines if you prefer hard deletion:
                    // userRepository.delete(user);

                    logger.info("Successfully processed deletion for user: {} ({})", user.getUsername(), userId);
                } else {
                    logger.warn("User not found for deletion: {}", userId);
                }
            }
        } catch (Exception e) {
            logger.error("Error processing user deletion webhook", e);
            throw new RuntimeException("Failed to process deletion webhook", e);
        }
    }

    /**
     * Verify webhook signature (for security)
     * Meta sends a signature to verify the webhook is authentic
     */
    public boolean verifyWebhookSignature(String payload, String signature, String appSecret) {
        try {
            // Meta uses HMAC-SHA256 to sign the payload
            // The signature format is: sha256=<hash>
            if (signature == null || !signature.startsWith("sha256=")) {
                logger.warn("Invalid signature format");
                return false;
            }

            String expectedSignature = signature.substring(7); // Remove "sha256=" prefix

            // Calculate HMAC-SHA256 of the payload using app secret
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                    appSecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] hash = mac.doFinal(payload.getBytes());
            String calculatedSignature = bytesToHex(hash);

            boolean isValid = calculatedSignature.equals(expectedSignature);

            if (!isValid) {
                logger.warn("Webhook signature verification failed. Expected: {}, Calculated: {}",
                        expectedSignature, calculatedSignature);
            }

            return isValid;

        } catch (Exception e) {
            logger.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}