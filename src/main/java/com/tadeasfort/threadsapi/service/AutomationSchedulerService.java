package com.tadeasfort.threadsapi.service;

import com.tadeasfort.threadsapi.entity.KeywordSubscription;
import com.tadeasfort.threadsapi.entity.User;
import com.tadeasfort.threadsapi.repository.KeywordSubscriptionRepository;
import com.tadeasfort.threadsapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutomationSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(AutomationSchedulerService.class);

    @Autowired
    private ThreadsKeywordSearchService keywordSearchService;

    @Autowired
    private KeywordSubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Process keyword subscriptions every hour
     * This job runs every hour and processes subscriptions that are due for search
     */
    @Scheduled(fixedRate = 3600000) // Every hour (3600000 ms)
    public void processKeywordSubscriptions() {
        logger.info("Starting scheduled keyword subscription processing");

        try {
            // Find subscriptions that are ready for search
            LocalDateTime cutoffTime = LocalDateTime.now();
            List<KeywordSubscription> readySubscriptions = subscriptionRepository
                    .findSubscriptionsReadyForSearch(cutoffTime);

            logger.info("Found {} subscriptions ready for processing", readySubscriptions.size());

            // Group subscriptions by user to batch process
            readySubscriptions.stream()
                    .collect(java.util.stream.Collectors.groupingBy(KeywordSubscription::getUserId))
                    .forEach(this::processUserSubscriptions);

            logger.info("Completed scheduled keyword subscription processing");

        } catch (Exception e) {
            logger.error("Error during scheduled keyword subscription processing: {}", e.getMessage(), e);
        }
    }

    /**
     * Process engagement analysis daily
     * This job runs daily and analyzes discovered posts for trends and insights
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    public void processEngagementAnalysis() {
        logger.info("Starting scheduled engagement analysis");

        try {
            // This would include:
            // 1. Calculate trending keywords
            // 2. Update engagement scores for older posts
            // 3. Generate insights and recommendations
            // 4. Clean up old data

            logger.info("Engagement analysis completed");

        } catch (Exception e) {
            logger.error("Error during engagement analysis: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up old data weekly
     */
    @Scheduled(cron = "0 0 3 * * SUN") // Weekly on Sunday at 3 AM
    public void cleanupOldData() {
        logger.info("Starting weekly data cleanup");

        try {
            // Clean up discovered posts older than 30 days
            // Clean up completed automation jobs older than 7 days
            // Archive old insights data

            logger.info("Data cleanup completed");

        } catch (Exception e) {
            logger.error("Error during data cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Process subscriptions for a specific user
     */
    private void processUserSubscriptions(String userId, List<KeywordSubscription> subscriptions) {
        try {
            // Get user's access token
            User user = userRepository.findByThreadsUserId(userId).orElse(null);
            if (user == null || user.getAccessToken() == null) {
                logger.warn("User {} not found or has no access token, skipping subscriptions", userId);
                return;
            }

            logger.info("Processing {} subscriptions for user {}", subscriptions.size(), userId);

            // Check rate limits before processing
            if (!keywordSearchService.checkRateLimit(userId)) {
                logger.warn("Rate limit exceeded for user {}, skipping subscription processing", userId);
                return;
            }

            // Process each subscription
            for (KeywordSubscription subscription : subscriptions) {
                try {
                    // Check if this specific subscription is due
                    if (isSubscriptionDue(subscription)) {
                        logger.debug("Processing subscription for keyword: {}", subscription.getKeyword());

                        // Perform the keyword search
                        keywordSearchService.searchKeyword(
                                userId,
                                subscription.getKeyword(),
                                user.getAccessToken(),
                                subscription.getSearchType());

                        // Update subscription
                        subscription.setLastSearchAt(LocalDateTime.now());
                        subscription.setTotalSearches(subscription.getTotalSearches() + 1);
                        subscriptionRepository.save(subscription);

                        // Check rate limits after each search
                        if (!keywordSearchService.checkRateLimit(userId)) {
                            logger.warn("Rate limit reached for user {}, stopping further processing", userId);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing subscription {} for user {}: {}",
                            subscription.getKeyword(), userId, e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error processing subscriptions for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Check if a subscription is due for search based on its frequency settings
     */
    private boolean isSubscriptionDue(KeywordSubscription subscription) {
        if (subscription.getLastSearchAt() == null) {
            return true; // Never searched before
        }

        LocalDateTime nextSearchTime = subscription.getLastSearchAt()
                .plusHours(subscription.getSearchFrequencyHours());

        return LocalDateTime.now().isAfter(nextSearchTime);
    }

    /**
     * Manual trigger for processing a specific user's subscriptions
     */
    public void processUserSubscriptionsManually(String userId) {
        logger.info("Manually triggering subscription processing for user {}", userId);

        List<KeywordSubscription> subscriptions = subscriptionRepository
                .findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);

        if (!subscriptions.isEmpty()) {
            processUserSubscriptions(userId, subscriptions);
        } else {
            logger.info("No active subscriptions found for user {}", userId);
        }
    }

    /**
     * Get automation statistics
     */
    public AutomationStats getAutomationStats() {
        // This would return statistics about:
        // - Total active subscriptions
        // - Total searches performed today
        // - Total posts discovered
        // - Average engagement scores
        // - Top performing keywords

        return new AutomationStats();
    }

    // Inner class for automation statistics
    public static class AutomationStats {
        private long totalActiveSubscriptions;
        private long searchesToday;
        private long postsDiscovered;
        private double averageEngagementScore;

        // Getters and setters
        public long getTotalActiveSubscriptions() {
            return totalActiveSubscriptions;
        }

        public void setTotalActiveSubscriptions(long totalActiveSubscriptions) {
            this.totalActiveSubscriptions = totalActiveSubscriptions;
        }

        public long getSearchesToday() {
            return searchesToday;
        }

        public void setSearchesToday(long searchesToday) {
            this.searchesToday = searchesToday;
        }

        public long getPostsDiscovered() {
            return postsDiscovered;
        }

        public void setPostsDiscovered(long postsDiscovered) {
            this.postsDiscovered = postsDiscovered;
        }

        public double getAverageEngagementScore() {
            return averageEngagementScore;
        }

        public void setAverageEngagementScore(double averageEngagementScore) {
            this.averageEngagementScore = averageEngagementScore;
        }
    }
}