package com.tadeasfort.threadsapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tadeasfort.threadsapi.entity.DiscoveredPost;
import com.tadeasfort.threadsapi.entity.InteractionQueue;
import com.tadeasfort.threadsapi.entity.KeywordSubscription;
import com.tadeasfort.threadsapi.entity.ThreadsPost;
import com.tadeasfort.threadsapi.repository.DiscoveredPostRepository;
import com.tadeasfort.threadsapi.repository.KeywordSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class ThreadsKeywordSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ThreadsKeywordSearchService.class);
    private static final String THREADS_API_BASE_URL = "https://graph.threads.net/v1.0";

    // Rate limiting: 2,200 queries per 24 hours per user
    private static final int MAX_QUERIES_PER_DAY = 2200;
    private static final long RATE_LIMIT_WINDOW_MS = 24 * 60 * 60 * 1000; // 24 hours

    // Rate limit tracking per user
    private final ConcurrentHashMap<String, AtomicInteger> userQueryCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> userQueryResetTimes = new ConcurrentHashMap<>();

    @Autowired
    private KeywordSubscriptionRepository subscriptionRepository;

    @Autowired
    private DiscoveredPostRepository discoveredPostRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InteractionQueueService queueService;

    /**
     * Search for posts using a specific keyword
     * 
     * IMPORTANT: The Threads API keyword search endpoint searches PUBLIC posts
     * across all users,
     * not just the authenticated user's posts. However, engagement metrics (likes,
     * views, etc.)
     * are only available for posts owned by the authenticated user due to privacy
     * restrictions.
     * 
     * This means:
     * 1. Public discovery works correctly - finds posts from any user matching the
     * keyword
     * 2. Engagement metrics will only be available for the authenticated user's own
     * posts
     * 3. Other users' posts will have engagement metrics set to 0 for privacy
     * reasons
     * 
     * This is the expected behavior according to Meta's Threads API documentation
     * and
     * privacy policies. For true content discovery, focus on the post content and
     * metadata
     * rather than engagement metrics for posts from other users.
     */
    public List<DiscoveredPost> searchKeyword(String userId, String keyword, String accessToken,
            KeywordSubscription.SearchType searchType) {

        // Check rate limits
        if (!checkRateLimit(userId)) {
            logger.warn("Rate limit exceeded for user {}", userId);
            throw new RuntimeException("Daily keyword search limit exceeded (2,200 queries per 24 hours)");
        }

        try {
            String url = UriComponentsBuilder.fromUriString(THREADS_API_BASE_URL + "/keyword_search")
                    .queryParam("q", keyword)
                    .queryParam("search_type", mapSearchTypeToApiValue(searchType))
                    .queryParam("fields",
                            "id,text,media_type,permalink,timestamp,username,has_replies,is_quote_post,is_reply")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            logger.info("Searching keyword '{}' for user {} with search type {}", keyword, userId, searchType);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // Increment query count
                incrementQueryCount(userId);

                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode dataArray = jsonResponse.get("data");

                List<DiscoveredPost> discoveredPosts = new ArrayList<>();

                if (dataArray != null && dataArray.isArray()) {
                    for (JsonNode postNode : dataArray) {
                        DiscoveredPost discoveredPost = parseDiscoveredPost(postNode, keyword, userId);
                        if (discoveredPost != null) {
                            // Check for duplicates
                            if (!discoveredPostRepository.existsByPostIdAndUserIdAndKeyword(
                                    discoveredPost.getPostId(), userId, keyword)) {

                                // Try to fetch engagement metrics for public posts
                                try {
                                    fetchEngagementMetrics(discoveredPost, accessToken);
                                } catch (Exception e) {
                                    logger.debug("Could not fetch engagement metrics for post {}: {}",
                                            discoveredPost.getPostId(), e.getMessage());
                                    // Set default values if metrics can't be fetched
                                    discoveredPost.setLikesCount(0L);
                                    discoveredPost.setRepliesCount(0L);
                                    discoveredPost.setRepostsCount(0L);
                                    discoveredPost.setQuotesCount(0L);
                                    discoveredPost.setViewsCount(0L);
                                }

                                // Calculate engagement score
                                discoveredPost.calculateEngagementScore();

                                // Save discovered post
                                discoveredPost = discoveredPostRepository.save(discoveredPost);
                                discoveredPosts.add(discoveredPost);

                                logger.debug("Discovered new post: {} (score: {})",
                                        discoveredPost.getPostId(), discoveredPost.getEngagementScore());

                                // Auto-queue high engagement posts
                                if (discoveredPost.getEngagementScore() > 100.0) {
                                    try {
                                        InteractionQueue.InteractionType interactionType = determineInteractionType(
                                                discoveredPost.getEngagementScore());
                                        queueService.queueDiscoveredPost(discoveredPost, interactionType);
                                        logger.debug("Auto-queued post {} for {} interaction",
                                                discoveredPost.getPostId(), interactionType);
                                    } catch (Exception e) {
                                        logger.warn("Failed to auto-queue post {}: {}",
                                                discoveredPost.getPostId(), e.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }

                logger.info("Found {} new posts for keyword '{}' (user: {})",
                        discoveredPosts.size(), keyword, userId);
                return discoveredPosts;
            } else {
                logger.error("Failed to search keyword '{}': HTTP {}", keyword, response.getStatusCode());
                throw new RuntimeException("Failed to search keyword: " + response.getStatusCode());
            }

        } catch (Exception e) {
            logger.error("Error searching keyword '{}' for user {}: {}", keyword, userId, e.getMessage(), e);
            throw new RuntimeException("Keyword search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process all active keyword subscriptions for a user
     */
    public void processUserKeywordSubscriptions(String userId, String accessToken) {
        List<KeywordSubscription> subscriptions = subscriptionRepository
                .findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);

        logger.info("Processing {} keyword subscriptions for user {}", subscriptions.size(), userId);

        for (KeywordSubscription subscription : subscriptions) {
            try {
                // Check if subscription is due for search
                if (isSubscriptionDueForSearch(subscription)) {
                    List<DiscoveredPost> discoveredPosts = searchKeyword(
                            userId,
                            subscription.getKeyword(),
                            accessToken,
                            subscription.getSearchType());

                    // Update subscription statistics
                    subscription.setLastSearchAt(LocalDateTime.now());
                    subscription.setTotalSearches(subscription.getTotalSearches() + 1);
                    subscription.setTotalPostsFound(subscription.getTotalPostsFound() + discoveredPosts.size());
                    subscriptionRepository.save(subscription);

                    logger.info("Processed subscription for keyword '{}': found {} posts",
                            subscription.getKeyword(), discoveredPosts.size());
                }
            } catch (Exception e) {
                logger.error("Error processing subscription for keyword '{}': {}",
                        subscription.getKeyword(), e.getMessage());
            }
        }
    }

    /**
     * Get discovered posts for a user with optional filtering
     */
    public List<DiscoveredPost> getDiscoveredPosts(String userId, String keyword, Double minEngagementScore,
            LocalDateTime since, int limit) {

        if (keyword != null && minEngagementScore != null) {
            return discoveredPostRepository.findPostsAboveThreshold(userId, minEngagementScore)
                    .stream()
                    .filter(post -> post.getKeyword().equals(keyword))
                    .limit(limit)
                    .toList();
        } else if (keyword != null) {
            return discoveredPostRepository.findByUserIdAndKeywordOrderByEngagementScoreDesc(
                    userId, keyword, org.springframework.data.domain.PageRequest.of(0, limit))
                    .getContent();
        } else if (minEngagementScore != null) {
            return discoveredPostRepository.findPostsAboveThreshold(userId, minEngagementScore)
                    .stream()
                    .limit(limit)
                    .toList();
        } else {
            return discoveredPostRepository.findByUserIdOrderByDiscoveredAtDesc(
                    userId, org.springframework.data.domain.PageRequest.of(0, limit))
                    .getContent();
        }
    }

    /**
     * Get trending posts across all keywords
     */
    public List<DiscoveredPost> getTrendingPosts(Double minEngagementScore, int hoursBack, int limit) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursBack);
        return discoveredPostRepository.findTrendingPosts(since, minEngagementScore)
                .stream()
                .limit(limit)
                .toList();
    }

    /**
     * Get keyword performance analytics
     */
    public List<Object[]> getKeywordPerformance(String userId) {
        return discoveredPostRepository.getKeywordPerformanceSummary(userId);
    }

    /**
     * Check if user has remaining API quota
     */
    public boolean checkRateLimit(String userId) {
        long currentTime = System.currentTimeMillis();

        // Reset counter if 24 hours have passed
        Long resetTime = userQueryResetTimes.get(userId);
        if (resetTime == null || currentTime > resetTime) {
            userQueryCounts.put(userId, new AtomicInteger(0));
            userQueryResetTimes.put(userId, currentTime + RATE_LIMIT_WINDOW_MS);
        }

        AtomicInteger count = userQueryCounts.get(userId);
        return count == null || count.get() < MAX_QUERIES_PER_DAY;
    }

    /**
     * Get remaining API quota for user
     */
    public int getRemainingQuota(String userId) {
        AtomicInteger count = userQueryCounts.get(userId);
        if (count == null) {
            return MAX_QUERIES_PER_DAY;
        }
        return Math.max(0, MAX_QUERIES_PER_DAY - count.get());
    }

    // Private helper methods

    private void incrementQueryCount(String userId) {
        userQueryCounts.computeIfAbsent(userId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    private boolean isSubscriptionDueForSearch(KeywordSubscription subscription) {
        if (subscription.getLastSearchAt() == null) {
            return true; // Never searched before
        }

        LocalDateTime nextSearchTime = subscription.getLastSearchAt()
                .plusHours(subscription.getSearchFrequencyHours());
        return LocalDateTime.now().isAfter(nextSearchTime);
    }

    private DiscoveredPost parseDiscoveredPost(JsonNode postNode, String keyword, String userId) {
        try {
            String postId = postNode.get("id").asText();

            DiscoveredPost discoveredPost = new DiscoveredPost(postId, keyword, userId);

            if (postNode.has("username")) {
                discoveredPost.setUsername(postNode.get("username").asText());
            }

            if (postNode.has("text")) {
                discoveredPost.setText(postNode.get("text").asText());
            }

            if (postNode.has("media_type")) {
                String mediaType = postNode.get("media_type").asText();
                discoveredPost.setMediaType(parseMediaType(mediaType));
            }

            if (postNode.has("permalink")) {
                discoveredPost.setPermalink(postNode.get("permalink").asText());
            }

            if (postNode.has("timestamp")) {
                String timestamp = postNode.get("timestamp").asText();
                discoveredPost.setPostTimestamp(parseThreadsTimestamp(timestamp));
            }

            if (postNode.has("has_replies")) {
                discoveredPost.setHasReplies(postNode.get("has_replies").asBoolean());
            }

            if (postNode.has("is_quote_post")) {
                discoveredPost.setIsQuotePost(postNode.get("is_quote_post").asBoolean());
            }

            if (postNode.has("is_reply")) {
                discoveredPost.setIsReply(postNode.get("is_reply").asBoolean());
            }

            return discoveredPost;

        } catch (Exception e) {
            logger.error("Error parsing discovered post: {}", e.getMessage());
            return null;
        }
    }

    private ThreadsPost.MediaType parseMediaType(String mediaType) {
        return switch (mediaType.toUpperCase()) {
            case "TEXT" -> ThreadsPost.MediaType.TEXT_POST;
            case "IMAGE" -> ThreadsPost.MediaType.IMAGE;
            case "VIDEO" -> ThreadsPost.MediaType.VIDEO;
            case "CAROUSEL_ALBUM" -> ThreadsPost.MediaType.CAROUSEL_ALBUM;
            default -> ThreadsPost.MediaType.TEXT_POST;
        };
    }

    private LocalDateTime parseThreadsTimestamp(String timestamp) {
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));
            return offsetDateTime.toLocalDateTime();
        } catch (Exception e) {
            logger.warn("Failed to parse timestamp '{}', using current time", timestamp);
            return LocalDateTime.now();
        }
    }

    private String mapSearchTypeToApiValue(KeywordSubscription.SearchType searchType) {
        return switch (searchType) {
            case TOP -> "TOP";
            case RECENT -> "RECENT";
        };
    }

    private InteractionQueue.InteractionType determineInteractionType(Double engagementScore) {
        if (engagementScore > 500) {
            return InteractionQueue.InteractionType.REPLY; // High engagement deserves a reply
        } else if (engagementScore > 200) {
            return InteractionQueue.InteractionType.QUOTE; // Medium engagement gets a quote
        } else {
            return InteractionQueue.InteractionType.LIKE; // Low engagement gets a like
        }
    }

    /**
     * Fetch engagement metrics for a discovered post
     */
    private void fetchEngagementMetrics(DiscoveredPost discoveredPost, String accessToken) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(THREADS_API_BASE_URL + "/" + discoveredPost.getPostId() + "/insights")
                    .queryParam("metric", "views,likes,replies,reposts,quotes")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode dataArray = jsonResponse.get("data");

                if (dataArray != null && dataArray.isArray()) {
                    for (JsonNode metricNode : dataArray) {
                        String metricName = metricNode.get("name").asText();
                        JsonNode valuesArray = metricNode.get("values");

                        if (valuesArray != null && valuesArray.isArray() && valuesArray.size() > 0) {
                            long value = valuesArray.get(0).get("value").asLong();

                            switch (metricName.toLowerCase()) {
                                case "views":
                                    discoveredPost.setViewsCount(value);
                                    break;
                                case "likes":
                                    discoveredPost.setLikesCount(value);
                                    break;
                                case "replies":
                                    discoveredPost.setRepliesCount(value);
                                    break;
                                case "reposts":
                                    discoveredPost.setRepostsCount(value);
                                    break;
                                case "quotes":
                                    discoveredPost.setQuotesCount(value);
                                    break;
                            }
                        }
                    }
                }
            } else {
                logger.debug("Could not fetch insights for post {}: HTTP {}",
                        discoveredPost.getPostId(), response.getStatusCode());
                // Set default values
                setDefaultEngagementMetrics(discoveredPost);
            }
        } catch (Exception e) {
            logger.debug("Error fetching engagement metrics for post {}: {}",
                    discoveredPost.getPostId(), e.getMessage());
            // Set default values
            setDefaultEngagementMetrics(discoveredPost);
        }
    }

    /**
     * Set default engagement metrics when they can't be fetched
     */
    private void setDefaultEngagementMetrics(DiscoveredPost discoveredPost) {
        discoveredPost.setLikesCount(0L);
        discoveredPost.setRepliesCount(0L);
        discoveredPost.setRepostsCount(0L);
        discoveredPost.setQuotesCount(0L);
        discoveredPost.setViewsCount(0L);
    }
}