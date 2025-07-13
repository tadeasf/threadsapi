package com.tadeasfort.threadsapi.controller;

import com.tadeasfort.threadsapi.dto.CreatePostRequest;
import com.tadeasfort.threadsapi.dto.PublishPostRequest;
import com.tadeasfort.threadsapi.dto.ThreadsPostResponse;
import com.tadeasfort.threadsapi.entity.DiscoveredPost;
import com.tadeasfort.threadsapi.entity.InteractionQueue;
import com.tadeasfort.threadsapi.entity.KeywordSubscription;
import com.tadeasfort.threadsapi.repository.DiscoveredPostRepository;
import com.tadeasfort.threadsapi.repository.KeywordSubscriptionRepository;
import com.tadeasfort.threadsapi.service.AutomationSchedulerService;
import com.tadeasfort.threadsapi.service.InteractionQueueService;
import com.tadeasfort.threadsapi.service.ThreadsApiClient;
import com.tadeasfort.threadsapi.service.ThreadsKeywordSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/automation")
@Tag(name = "Automation", description = "Automation and keyword monitoring APIs")
public class AutomationController {

    private static final Logger logger = LoggerFactory.getLogger(AutomationController.class);

    @Autowired
    private KeywordSubscriptionRepository subscriptionRepository;

    @Autowired
    private DiscoveredPostRepository discoveredPostRepository;

    @Autowired
    private ThreadsKeywordSearchService keywordSearchService;

    @Autowired
    private AutomationSchedulerService schedulerService;

    @Autowired
    private InteractionQueueService queueService;

    @Autowired
    private ThreadsApiClient threadsApiClient;

    // Keyword Subscription Management

    @PostMapping("/subscriptions")
    @Operation(summary = "Create a new keyword subscription")
    public ResponseEntity<?> createSubscription(
            @RequestParam String userId,
            @RequestParam String accessToken,
            @RequestBody CreateSubscriptionRequest request) {
        try {
            // Set userId and accessToken from query parameters
            request.setUserId(userId);
            request.setAccessToken(accessToken);

            // Check if subscription already exists
            if (subscriptionRepository.existsByUserIdAndKeywordAndIsActiveTrue(
                    request.getUserId(), request.getKeyword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Subscription for this keyword already exists"));
            }

            KeywordSubscription subscription = new KeywordSubscription(request.getUserId(), request.getKeyword());

            if (request.getSearchType() != null) {
                subscription.setSearchType(request.getSearchType());
            }
            // Handle legacy search type strings from frontend
            if (request.getSearchTypeString() != null) {
                subscription.setSearchType(mapLegacySearchType(request.getSearchTypeString()));
            }
            if (request.getEngagementThreshold() != null) {
                subscription.setEngagementThreshold(request.getEngagementThreshold());
            }
            if (request.getSearchFrequencyHours() != null) {
                subscription.setSearchFrequencyHours(request.getSearchFrequencyHours());
            }
            if (request.getMaxPostsPerSearch() != null) {
                subscription.setMaxPostsPerSearch(request.getMaxPostsPerSearch());
            }

            subscription = subscriptionRepository.save(subscription);

            logger.info("Created keyword subscription: {} for user {}",
                    request.getKeyword(), request.getUserId());

            return ResponseEntity.ok(subscription);

        } catch (Exception e) {
            logger.error("Error creating subscription: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create subscription"));
        }
    }

    @GetMapping("/subscriptions/{userId}")
    @Operation(summary = "Get user's keyword subscriptions")
    public ResponseEntity<List<KeywordSubscription>> getUserSubscriptions(@PathVariable String userId) {
        try {
            List<KeywordSubscription> subscriptions = subscriptionRepository
                    .findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(subscriptions);
        } catch (Exception e) {
            logger.error("Error fetching subscriptions for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Update a keyword subscription")
    public ResponseEntity<?> updateSubscription(@PathVariable Long subscriptionId,
            @RequestBody UpdateSubscriptionRequest request) {
        try {
            Optional<KeywordSubscription> optSubscription = subscriptionRepository.findById(subscriptionId);
            if (optSubscription.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            KeywordSubscription subscription = optSubscription.get();

            if (request.getIsActive() != null) {
                subscription.setIsActive(request.getIsActive());
            }
            if (request.getEngagementThreshold() != null) {
                subscription.setEngagementThreshold(request.getEngagementThreshold());
            }
            if (request.getSearchFrequencyHours() != null) {
                subscription.setSearchFrequencyHours(request.getSearchFrequencyHours());
            }
            if (request.getMaxPostsPerSearch() != null) {
                subscription.setMaxPostsPerSearch(request.getMaxPostsPerSearch());
            }
            if (request.getSearchType() != null) {
                subscription.setSearchType(request.getSearchType());
            }

            subscription = subscriptionRepository.save(subscription);

            logger.info("Updated subscription {} for user {}",
                    subscriptionId, subscription.getUserId());

            return ResponseEntity.ok(subscription);

        } catch (Exception e) {
            logger.error("Error updating subscription {}: {}", subscriptionId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update subscription"));
        }
    }

    @DeleteMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Delete a keyword subscription")
    public ResponseEntity<?> deleteSubscription(@PathVariable Long subscriptionId) {
        try {
            if (!subscriptionRepository.existsById(subscriptionId)) {
                return ResponseEntity.notFound().build();
            }

            subscriptionRepository.deleteById(subscriptionId);

            logger.info("Deleted subscription {}", subscriptionId);
            return ResponseEntity.ok(Map.of("message", "Subscription deleted successfully"));

        } catch (Exception e) {
            logger.error("Error deleting subscription {}: {}", subscriptionId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to delete subscription"));
        }
    }

    // Discovered Posts

    @GetMapping("/discovered-posts/{userId}")
    @Operation(summary = "Get discovered posts for a user")
    public ResponseEntity<List<DiscoveredPost>> getDiscoveredPosts(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double minEngagementScore,
            @RequestParam(defaultValue = "engagementScore") String sortBy) {

        try {
            List<DiscoveredPost> posts;

            if (keyword != null) {
                Pageable pageable = PageRequest.of(0, limit);
                posts = discoveredPostRepository.findByUserIdAndKeywordOrderByEngagementScoreDesc(
                        userId, keyword, pageable).getContent();
            } else {
                posts = keywordSearchService.getDiscoveredPosts(userId, keyword, minEngagementScore, null, limit);
            }

            return ResponseEntity.ok(posts);

        } catch (Exception e) {
            logger.error("Error fetching discovered posts for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/discovered-posts/{userId}/top")
    @Operation(summary = "Get top discovered posts by engagement score")
    public ResponseEntity<Page<DiscoveredPost>> getTopDiscoveredPosts(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<DiscoveredPost> posts = discoveredPostRepository
                    .findTopPostsByEngagementScore(userId, pageable);

            return ResponseEntity.ok(posts);

        } catch (Exception e) {
            logger.error("Error fetching top posts for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/trending-posts")
    @Operation(summary = "Get trending posts across all users")
    public ResponseEntity<List<DiscoveredPost>> getTrendingPosts(
            @RequestParam(defaultValue = "100.0") Double minEngagementScore,
            @RequestParam(defaultValue = "24") int hoursBack,
            @RequestParam(defaultValue = "50") int limit) {

        try {
            List<DiscoveredPost> trendingPosts = keywordSearchService
                    .getTrendingPosts(minEngagementScore, hoursBack, limit);

            return ResponseEntity.ok(trendingPosts);

        } catch (Exception e) {
            logger.error("Error fetching trending posts: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    // Manual Search

    @PostMapping("/search")
    @Operation(summary = "Manually trigger a keyword search")
    public ResponseEntity<?> manualSearch(@RequestBody ManualSearchRequest request) {
        try {
            // Check rate limits
            if (!keywordSearchService.checkRateLimit(request.getUserId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Daily search limit exceeded"));
            }

            List<DiscoveredPost> results = keywordSearchService.searchKeyword(
                    request.getUserId(),
                    request.getKeyword(),
                    request.getAccessToken(),
                    request.getSearchType() != null ? request.getSearchType() : KeywordSubscription.SearchType.TOP);

            return ResponseEntity.ok(Map.of(
                    "message", "Search completed successfully",
                    "postsFound", results.size(),
                    "remainingQuota", keywordSearchService.getRemainingQuota(request.getUserId()),
                    "posts", results));

        } catch (Exception e) {
            logger.error("Error performing manual search: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Search failed: " + e.getMessage()));
        }
    }

    // Analytics

    @GetMapping("/analytics/{userId}")
    @Operation(summary = "Get automation analytics for a user")
    public ResponseEntity<?> getAnalytics(@PathVariable String userId) {
        try {
            // Get subscription statistics
            long activeSubscriptions = subscriptionRepository.countByUserIdAndIsActiveTrue(userId);
            Object[] keywordStats = subscriptionRepository.getUserKeywordStats(userId);

            // Get discovered posts statistics
            long postsToday = discoveredPostRepository.countPostsDiscoveredToday(userId, LocalDateTime.now());
            List<Object[]> keywordPerformance = discoveredPostRepository.getKeywordPerformanceSummary(userId);

            // Get rate limit info
            int remainingQuota = keywordSearchService.getRemainingQuota(userId);

            return ResponseEntity.ok(Map.of(
                    "activeSubscriptions", activeSubscriptions,
                    "totalSearches", keywordStats != null && keywordStats[0] != null ? keywordStats[0] : 0,
                    "totalPostsFound", keywordStats != null && keywordStats[1] != null ? keywordStats[1] : 0,
                    "postsDiscoveredToday", postsToday,
                    "remainingDailyQuota", remainingQuota,
                    "keywordPerformance", keywordPerformance));

        } catch (Exception e) {
            logger.error("Error fetching analytics for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch analytics"));
        }
    }

    @PostMapping("/trigger/{userId}")
    @Operation(summary = "Manually trigger subscription processing for a user")
    public ResponseEntity<?> triggerProcessing(@PathVariable String userId) {
        try {
            schedulerService.processUserSubscriptionsManually(userId);

            return ResponseEntity.ok(Map.of(
                    "message", "Subscription processing triggered successfully"));

        } catch (Exception e) {
            logger.error("Error triggering processing for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to trigger processing"));
        }
    }

    // Queue Management

    @GetMapping("/queue/{userId}")
    @Operation(summary = "Get user's interaction queue")
    public ResponseEntity<Page<InteractionQueue>> getUserQueue(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<InteractionQueue> queueItems = queueService.getUserQueueItems(userId, pageable);
            return ResponseEntity.ok(queueItems);
        } catch (Exception e) {
            logger.error("Error fetching queue for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/queue")
    @Operation(summary = "Add item to interaction queue")
    public ResponseEntity<?> addToQueue(@RequestBody QueueItemRequest request) {
        try {
            InteractionQueue queueItem = queueService.queueInteraction(
                    request.getUserId(),
                    request.getPostId(),
                    request.getInteractionType(),
                    request.getEngagementScore(),
                    request.getReason());

            if (queueItem != null) {
                return ResponseEntity.ok(queueItem);
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Failed to queue item - may already exist or queue is full"));
            }
        } catch (Exception e) {
            logger.error("Error adding item to queue: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to add item to queue"));
        }
    }

    @PostMapping("/queue/{queueId}/complete")
    @Operation(summary = "Mark queue item as completed")
    public ResponseEntity<?> completeQueueItem(@PathVariable Long queueId, @RequestBody Map<String, String> request) {
        try {
            String result = request.getOrDefault("result", "Completed successfully");
            InteractionQueue updatedItem = queueService.markAsCompleted(queueId, result);

            if (updatedItem != null) {
                return ResponseEntity.ok(updatedItem);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error completing queue item {}: {}", queueId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to complete queue item"));
        }
    }

    @PostMapping("/queue/{queueId}/fail")
    @Operation(summary = "Mark queue item as failed")
    public ResponseEntity<?> failQueueItem(@PathVariable Long queueId, @RequestBody Map<String, String> request) {
        try {
            String error = request.getOrDefault("error", "Failed to process");
            InteractionQueue updatedItem = queueService.markAsFailed(queueId, error);

            if (updatedItem != null) {
                return ResponseEntity.ok(updatedItem);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error failing queue item {}: {}", queueId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to update queue item"));
        }
    }

    @DeleteMapping("/queue/{queueId}")
    @Operation(summary = "Cancel queue item")
    public ResponseEntity<?> cancelQueueItem(@PathVariable Long queueId, @RequestParam String userId) {
        try {
            boolean cancelled = queueService.cancelQueueItem(queueId, userId);

            if (cancelled) {
                return ResponseEntity.ok(Map.of("message", "Queue item cancelled successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Failed to cancel queue item - may not exist or not pending"));
            }
        } catch (Exception e) {
            logger.error("Error cancelling queue item {}: {}", queueId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to cancel queue item"));
        }
    }

    @GetMapping("/queue/{userId}/stats")
    @Operation(summary = "Get queue statistics for user")
    public ResponseEntity<?> getQueueStats(@PathVariable String userId) {
        try {
            Map<String, Object> stats = queueService.getQueueStatistics(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching queue stats for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch queue statistics"));
        }
    }

    @PostMapping("/queue/{userId}/auto-queue")
    @Operation(summary = "Auto-queue high engagement posts for user")
    public ResponseEntity<?> autoQueueHighEngagementPosts(
            @PathVariable String userId,
            @RequestParam(defaultValue = "100.0") double minEngagementScore) {

        try {
            queueService.autoQueueHighEngagementPosts(userId, minEngagementScore);
            return ResponseEntity.ok(Map.of(
                    "message", "Auto-queued high engagement posts successfully"));
        } catch (Exception e) {
            logger.error("Error auto-queuing posts for user {}: {}", userId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to auto-queue posts"));
        }
    }

    // Post Creation Endpoints

    @PostMapping("/posts/create")
    @Operation(summary = "Create a new post")
    public ResponseEntity<?> createPost(@RequestBody CreatePostRequest request) {
        try {
            // Validate post request
            if (request.getMediaType() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Media type is required"));
            }

            // Create post via API
            ThreadsPostResponse response = threadsApiClient.createPost(request.getAccessToken(), request);

            logger.info("Created post with ID: {} for user: {}", response.getId(), request.getUserId());

            return ResponseEntity.ok(Map.of(
                    "message", "Post created successfully",
                    "creationId", response.getId(),
                    "mediaType", request.getMediaType()));

        } catch (Exception e) {
            logger.error("Error creating post: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create post: " + e.getMessage()));
        }
    }

    @PostMapping("/posts/publish")
    @Operation(summary = "Publish a created post")
    public ResponseEntity<?> publishPost(@RequestBody PublishPostRequest request) {
        try {
            Map<String, Object> response = threadsApiClient.publishPost(request.getAccessToken(),
                    request.getCreationId());

            logger.info("Published post with creation ID: {} for user: {}", request.getCreationId(),
                    request.getUserId());

            return ResponseEntity.ok(Map.of(
                    "message", "Post published successfully",
                    "postId", response.get("id")));

        } catch (Exception e) {
            logger.error("Error publishing post: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to publish post: " + e.getMessage()));
        }
    }

    @PostMapping("/posts/create-and-publish")
    @Operation(summary = "Create and immediately publish a post")
    public ResponseEntity<?> createAndPublishPost(@RequestBody CreatePostRequest request) {
        try {
            // Create the post
            ThreadsPostResponse createResponse = threadsApiClient.createPost(request.getAccessToken(), request);

            // Publish the post
            Map<String, Object> publishResponse = threadsApiClient.publishPost(request.getAccessToken(),
                    createResponse.getId());

            logger.info("Created and published post with ID: {} for user: {}", publishResponse.get("id"),
                    request.getUserId());

            return ResponseEntity.ok(Map.of(
                    "message", "Post created and published successfully",
                    "postId", publishResponse.get("id"),
                    "creationId", createResponse.getId()));

        } catch (Exception e) {
            logger.error("Error creating and publishing post: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create and publish post: " + e.getMessage()));
        }
    }

    @GetMapping("/posts/templates")
    @Operation(summary = "Get post creation templates and examples")
    public ResponseEntity<?> getPostTemplates() {
        try {
            Map<String, Object> templates = Map.of(
                    "textPost", Map.of(
                            "media_type", "TEXT_POST",
                            "text", "Your text content here",
                            "description", "Simple text post"),
                    "imagePost", Map.of(
                            "media_type", "IMAGE",
                            "text", "Caption for your image",
                            "image_url", "https://example.com/image.jpg",
                            "alt_text", "Description of the image for accessibility",
                            "description", "Post with a single image"),
                    "videoPost", Map.of(
                            "media_type", "VIDEO",
                            "text", "Caption for your video",
                            "video_url", "https://example.com/video.mp4",
                            "description", "Post with a video"),
                    "carouselPost", Map.of(
                            "media_type", "CAROUSEL_ALBUM",
                            "text", "Caption for your carousel",
                            "children", List.of("child_media_id_1", "child_media_id_2"),
                            "description", "Post with multiple images/videos"),
                    "replyPost", Map.of(
                            "media_type", "TEXT_POST",
                            "text", "Your reply text",
                            "reply_to_id", "original_post_id",
                            "description", "Reply to another post"),
                    "quotePost", Map.of(
                            "media_type", "TEXT_POST",
                            "text", "Your comment on the quoted post",
                            "quote_post_id", "post_to_quote_id",
                            "description", "Quote another post with your commentary"),
                    "locationPost", Map.of(
                            "media_type", "TEXT_POST",
                            "text", "Check out this location!",
                            "location_name", "San Francisco, CA",
                            "description", "Post with location tagging"));

            return ResponseEntity.ok(Map.of(
                    "templates", templates,
                    "supportedMediaTypes", List.of("TEXT_POST", "IMAGE", "VIDEO", "CAROUSEL_ALBUM"),
                    "optionalFeatures", List.of("location_tagging", "alt_text", "reply_to", "quote_post",
                            "allow_commenting", "hide_like_view_counts")));

        } catch (Exception e) {
            logger.error("Error fetching post templates: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch post templates"));
        }
    }

    // Request DTOs

    public static class QueueItemRequest {
        private String userId;
        private String postId;
        private InteractionQueue.InteractionType interactionType;
        private Double engagementScore;
        private String reason;

        // Getters and setters
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPostId() {
            return postId;
        }

        public void setPostId(String postId) {
            this.postId = postId;
        }

        public InteractionQueue.InteractionType getInteractionType() {
            return interactionType;
        }

        public void setInteractionType(InteractionQueue.InteractionType interactionType) {
            this.interactionType = interactionType;
        }

        public Double getEngagementScore() {
            return engagementScore;
        }

        public void setEngagementScore(Double engagementScore) {
            this.engagementScore = engagementScore;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    public static class CreateSubscriptionRequest {
        private String userId;
        private String keyword;
        private KeywordSubscription.SearchType searchType;
        private Integer engagementThreshold;
        private Integer searchFrequencyHours;
        private Integer maxPostsPerSearch;
        private String searchTypeString; // Added for legacy support
        private String accessToken; // Added for access token

        // Getters and setters
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public KeywordSubscription.SearchType getSearchType() {
            return searchType;
        }

        public void setSearchType(KeywordSubscription.SearchType searchType) {
            this.searchType = searchType;
        }

        // Handle string-based searchType from frontend
        @com.fasterxml.jackson.annotation.JsonSetter("searchType")
        public void setSearchTypeFromString(String searchTypeString) {
            if (searchTypeString != null) {
                try {
                    // Try to parse as enum first
                    this.searchType = KeywordSubscription.SearchType.valueOf(searchTypeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // If enum parsing fails, map legacy values
                    this.searchType = mapLegacySearchTypeStatic(searchTypeString);
                }
            }
        }

        public Integer getEngagementThreshold() {
            return engagementThreshold;
        }

        public void setEngagementThreshold(Integer engagementThreshold) {
            this.engagementThreshold = engagementThreshold;
        }

        public Integer getSearchFrequencyHours() {
            return searchFrequencyHours;
        }

        public void setSearchFrequencyHours(Integer searchFrequencyHours) {
            this.searchFrequencyHours = searchFrequencyHours;
        }

        public Integer getMaxPostsPerSearch() {
            return maxPostsPerSearch;
        }

        public void setMaxPostsPerSearch(Integer maxPostsPerSearch) {
            this.maxPostsPerSearch = maxPostsPerSearch;
        }

        public String getSearchTypeString() {
            return searchTypeString;
        }

        public void setSearchTypeString(String searchTypeString) {
            this.searchTypeString = searchTypeString;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        // Static helper method for mapping legacy search types
        private static KeywordSubscription.SearchType mapLegacySearchTypeStatic(String searchTypeString) {
            switch (searchTypeString.toLowerCase()) {
                case "top":
                case "text":
                case "hashtag":
                case "mention":
                case "popular":
                    return KeywordSubscription.SearchType.TOP;
                case "recent":
                    return KeywordSubscription.SearchType.RECENT;
                default:
                    return KeywordSubscription.SearchType.TOP; // Default to TOP
            }
        }
    }

    public static class UpdateSubscriptionRequest {
        private Boolean isActive;
        private Integer engagementThreshold;
        private Integer searchFrequencyHours;
        private Integer maxPostsPerSearch;
        private KeywordSubscription.SearchType searchType;

        // Getters and setters
        public Boolean getIsActive() {
            return isActive;
        }

        public void setIsActive(Boolean isActive) {
            this.isActive = isActive;
        }

        public Integer getEngagementThreshold() {
            return engagementThreshold;
        }

        public void setEngagementThreshold(Integer engagementThreshold) {
            this.engagementThreshold = engagementThreshold;
        }

        public Integer getSearchFrequencyHours() {
            return searchFrequencyHours;
        }

        public void setSearchFrequencyHours(Integer searchFrequencyHours) {
            this.searchFrequencyHours = searchFrequencyHours;
        }

        public Integer getMaxPostsPerSearch() {
            return maxPostsPerSearch;
        }

        public void setMaxPostsPerSearch(Integer maxPostsPerSearch) {
            this.maxPostsPerSearch = maxPostsPerSearch;
        }

        public KeywordSubscription.SearchType getSearchType() {
            return searchType;
        }

        public void setSearchType(KeywordSubscription.SearchType searchType) {
            this.searchType = searchType;
        }
    }

    public static class ManualSearchRequest {
        private String userId;
        private String keyword;
        private String accessToken;
        private KeywordSubscription.SearchType searchType;

        // Getters and setters
        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public KeywordSubscription.SearchType getSearchType() {
            return searchType;
        }

        public void setSearchType(KeywordSubscription.SearchType searchType) {
            this.searchType = searchType;
        }

        // Handle string-based searchType from frontend
        @com.fasterxml.jackson.annotation.JsonSetter("searchType")
        public void setSearchTypeFromString(String searchTypeString) {
            if (searchTypeString != null) {
                try {
                    // Try to parse as enum first
                    this.searchType = KeywordSubscription.SearchType.valueOf(searchTypeString.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // If enum parsing fails, map legacy values
                    this.searchType = mapLegacySearchTypeStatic(searchTypeString);
                }
            }
        }

        // Static helper method for mapping legacy search types
        private static KeywordSubscription.SearchType mapLegacySearchTypeStatic(String searchTypeString) {
            switch (searchTypeString.toLowerCase()) {
                case "top":
                case "text":
                case "hashtag":
                case "mention":
                case "popular":
                    return KeywordSubscription.SearchType.TOP;
                case "recent":
                    return KeywordSubscription.SearchType.RECENT;
                default:
                    return KeywordSubscription.SearchType.TOP; // Default to TOP
            }
        }
    }

    // Helper method to map legacy search type strings to new enum values
    private KeywordSubscription.SearchType mapLegacySearchType(String searchTypeString) {
        switch (searchTypeString.toLowerCase()) {
            case "top":
            case "text":
            case "hashtag":
            case "mention":
            case "popular":
                return KeywordSubscription.SearchType.TOP;
            case "recent":
                return KeywordSubscription.SearchType.RECENT;
            default:
                return KeywordSubscription.SearchType.TOP; // Default to TOP
        }
    }
}