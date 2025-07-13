package com.tadeasfort.threadsapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ThreadsRateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(ThreadsRateLimitService.class);
    private static final String THREADS_API_BASE_URL = "https://graph.threads.net/v1.0";

    // Rate limit tracking per user
    private final ConcurrentHashMap<String, UserRateLimitInfo> userRateLimits = new ConcurrentHashMap<>();

    // Default limits based on Threads API documentation
    private static final int MIN_IMPRESSIONS = 10;
    private static final int CALLS_PER_IMPRESSION = 4800;
    private static final int POSTS_PER_24H = 250;
    private static final int REPLIES_PER_24H = 1000;
    private static final long CPU_TIME_PER_IMPRESSION = 720000;
    private static final long TOTAL_TIME_PER_IMPRESSION = 2880000;

    // Sliding window for rate limiting (24 hours)
    private static final long RATE_LIMIT_WINDOW_HOURS = 24;

    public static class UserRateLimitInfo {
        private final String userId;
        private final AtomicInteger callsInWindow = new AtomicInteger(0);
        private final AtomicInteger postsInWindow = new AtomicInteger(0);
        private final AtomicInteger repliesInWindow = new AtomicInteger(0);
        private final AtomicLong cpuTimeUsed = new AtomicLong(0);
        private final AtomicLong totalTimeUsed = new AtomicLong(0);
        private volatile LocalDateTime windowStart = LocalDateTime.now();
        private volatile int impressions = MIN_IMPRESSIONS; // Default to minimum
        private volatile LocalDateTime lastApiCall = LocalDateTime.now();

        public UserRateLimitInfo(String userId) {
            this.userId = userId;
        }

        // Getters
        public int getCallsInWindow() {
            return callsInWindow.get();
        }

        public int getPostsInWindow() {
            return postsInWindow.get();
        }

        public int getRepliesInWindow() {
            return repliesInWindow.get();
        }

        public long getCpuTimeUsed() {
            return cpuTimeUsed.get();
        }

        public long getTotalTimeUsed() {
            return totalTimeUsed.get();
        }

        public LocalDateTime getWindowStart() {
            return windowStart;
        }

        public int getImpressions() {
            return impressions;
        }

        public LocalDateTime getLastApiCall() {
            return lastApiCall;
        }

        // Calculated limits based on impressions
        public int getMaxCallsPerWindow() {
            return Math.max(MIN_IMPRESSIONS, impressions) * CALLS_PER_IMPRESSION;
        }

        public long getMaxCpuTime() {
            return Math.max(MIN_IMPRESSIONS, impressions) * CPU_TIME_PER_IMPRESSION;
        }

        public long getMaxTotalTime() {
            return Math.max(MIN_IMPRESSIONS, impressions) * TOTAL_TIME_PER_IMPRESSION;
        }

        public synchronized void resetWindowIfNeeded() {
            LocalDateTime now = LocalDateTime.now();
            if (ChronoUnit.HOURS.between(windowStart, now) >= RATE_LIMIT_WINDOW_HOURS) {
                windowStart = now;
                callsInWindow.set(0);
                postsInWindow.set(0);
                repliesInWindow.set(0);
                cpuTimeUsed.set(0);
                totalTimeUsed.set(0);
                logger.info("Rate limit window reset for user: {}", userId);
            }
        }

        public void recordApiCall() {
            resetWindowIfNeeded();
            callsInWindow.incrementAndGet();
            lastApiCall = LocalDateTime.now();
        }

        public void recordPost() {
            resetWindowIfNeeded();
            postsInWindow.incrementAndGet();
        }

        public void recordReply() {
            resetWindowIfNeeded();
            repliesInWindow.incrementAndGet();
        }

        public void recordCpuTime(long cpuTime) {
            cpuTimeUsed.addAndGet(cpuTime);
        }

        public void recordTotalTime(long totalTime) {
            totalTimeUsed.addAndGet(totalTime);
        }

        public void updateImpressions(int newImpressions) {
            this.impressions = Math.max(MIN_IMPRESSIONS, newImpressions);
        }
    }

    public static class RateLimitStatus {
        private final boolean allowed;
        private final String reason;
        private final long retryAfterSeconds;
        private final int remainingCalls;
        private final int remainingPosts;
        private final int remainingReplies;

        public RateLimitStatus(boolean allowed, String reason, long retryAfterSeconds,
                int remainingCalls, int remainingPosts, int remainingReplies) {
            this.allowed = allowed;
            this.reason = reason;
            this.retryAfterSeconds = retryAfterSeconds;
            this.remainingCalls = remainingCalls;
            this.remainingPosts = remainingPosts;
            this.remainingReplies = remainingReplies;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }

        public int getRemainingCalls() {
            return remainingCalls;
        }

        public int getRemainingPosts() {
            return remainingPosts;
        }

        public int getRemainingReplies() {
            return remainingReplies;
        }
    }

    /**
     * Check if an API call is allowed for the user
     */
    public RateLimitStatus checkApiCallLimit(String userId) {
        UserRateLimitInfo userInfo = getUserRateLimitInfo(userId);
        userInfo.resetWindowIfNeeded();

        int currentCalls = userInfo.getCallsInWindow();
        int maxCalls = userInfo.getMaxCallsPerWindow();

        if (currentCalls >= maxCalls) {
            long retryAfter = calculateRetryAfterSeconds(userInfo.getWindowStart());
            return new RateLimitStatus(false, "API call limit exceeded", retryAfter,
                    0, getRemainingPosts(userId), getRemainingReplies(userId));
        }

        return new RateLimitStatus(true, "OK", 0,
                maxCalls - currentCalls, getRemainingPosts(userId), getRemainingReplies(userId));
    }

    /**
     * Check if a post creation is allowed for the user
     */
    public RateLimitStatus checkPostLimit(String userId) {
        UserRateLimitInfo userInfo = getUserRateLimitInfo(userId);
        userInfo.resetWindowIfNeeded();

        int currentPosts = userInfo.getPostsInWindow();

        if (currentPosts >= POSTS_PER_24H) {
            long retryAfter = calculateRetryAfterSeconds(userInfo.getWindowStart());
            return new RateLimitStatus(false, "Post limit exceeded (250 posts per 24h)", retryAfter,
                    getRemainingCalls(userId), 0, getRemainingReplies(userId));
        }

        return new RateLimitStatus(true, "OK", 0,
                getRemainingCalls(userId), POSTS_PER_24H - currentPosts, getRemainingReplies(userId));
    }

    /**
     * Check if a reply creation is allowed for the user
     */
    public RateLimitStatus checkReplyLimit(String userId) {
        UserRateLimitInfo userInfo = getUserRateLimitInfo(userId);
        userInfo.resetWindowIfNeeded();

        int currentReplies = userInfo.getRepliesInWindow();

        if (currentReplies >= REPLIES_PER_24H) {
            long retryAfter = calculateRetryAfterSeconds(userInfo.getWindowStart());
            return new RateLimitStatus(false, "Reply limit exceeded (1000 replies per 24h)", retryAfter,
                    getRemainingCalls(userId), getRemainingPosts(userId), 0);
        }

        return new RateLimitStatus(true, "OK", 0,
                getRemainingCalls(userId), getRemainingPosts(userId), REPLIES_PER_24H - currentReplies);
    }

    /**
     * Record an API call for rate limiting
     */
    public void recordApiCall(String userId) {
        getUserRateLimitInfo(userId).recordApiCall();
    }

    /**
     * Record a post creation for rate limiting
     */
    public void recordPost(String userId) {
        UserRateLimitInfo userInfo = getUserRateLimitInfo(userId);
        userInfo.recordApiCall(); // Posts also count as API calls
        userInfo.recordPost();
    }

    /**
     * Record a reply creation for rate limiting
     */
    public void recordReply(String userId) {
        UserRateLimitInfo userInfo = getUserRateLimitInfo(userId);
        userInfo.recordApiCall(); // Replies also count as API calls
        userInfo.recordReply();
    }

    /**
     * Update user's impression count (affects rate limits)
     */
    public void updateUserImpressions(String userId, int impressions) {
        getUserRateLimitInfo(userId).updateImpressions(impressions);
        logger.info("Updated impressions for user {}: {}", userId, impressions);
    }

    /**
     * Get current rate limit status for a user
     */
    public UserRateLimitInfo getUserRateLimitStatus(String userId) {
        UserRateLimitInfo userInfo = getUserRateLimitInfo(userId);
        userInfo.resetWindowIfNeeded();
        return userInfo;
    }

    /**
     * Fetch publishing limits from Threads API
     */
    public void fetchAndUpdatePublishingLimits(String userId, String accessToken, RestTemplate restTemplate) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(THREADS_API_BASE_URL + "/" + userId + "/threads_publishing_limit")
                    .queryParam("fields", "quota_usage,config,reply_quota_usage,reply_config")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            // This would update our local tracking with actual API limits
            // Implementation depends on your JSON parsing setup
            logger.info("Fetching publishing limits for user: {}", userId);

        } catch (Exception e) {
            logger.warn("Failed to fetch publishing limits for user {}: {}", userId, e.getMessage());
        }
    }

    private UserRateLimitInfo getUserRateLimitInfo(String userId) {
        return userRateLimits.computeIfAbsent(userId, UserRateLimitInfo::new);
    }

    private int getRemainingCalls(String userId) {
        UserRateLimitInfo userInfo = getUserRateLimitInfo(userId);
        return Math.max(0, userInfo.getMaxCallsPerWindow() - userInfo.getCallsInWindow());
    }

    private int getRemainingPosts(String userId) {
        UserRateLimitInfo userInfo = getUserRateLimitInfo(userId);
        return Math.max(0, POSTS_PER_24H - userInfo.getPostsInWindow());
    }

    private int getRemainingReplies(String userId) {
        UserRateLimitInfo userInfo = getUserRateLimitInfo(userId);
        return Math.max(0, REPLIES_PER_24H - userInfo.getRepliesInWindow());
    }

    private long calculateRetryAfterSeconds(LocalDateTime windowStart) {
        LocalDateTime windowEnd = windowStart.plusHours(RATE_LIMIT_WINDOW_HOURS);
        return Math.max(0, ChronoUnit.SECONDS.between(LocalDateTime.now(), windowEnd));
    }

    /**
     * Clean up old rate limit entries (call periodically)
     */
    public void cleanupOldEntries() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(RATE_LIMIT_WINDOW_HOURS * 2);
        userRateLimits.entrySet().removeIf(entry -> entry.getValue().getLastApiCall().isBefore(cutoff));
    }
}