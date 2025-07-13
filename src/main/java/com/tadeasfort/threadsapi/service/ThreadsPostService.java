package com.tadeasfort.threadsapi.service;

import com.tadeasfort.threadsapi.entity.ThreadsPost;
import com.tadeasfort.threadsapi.repository.ThreadsPostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ThreadsPostService {

    private static final Logger logger = LoggerFactory.getLogger(ThreadsPostService.class);
    private static final String THREADS_API_BASE_URL = "https://graph.threads.net/v1.0";

    // Custom formatter for Threads API timestamps (e.g.,
    // "2025-06-22T13:00:27+0000")
    private static final DateTimeFormatter THREADS_TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Autowired
    private ThreadsPostRepository postsRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Retrieve and store user's posts from Threads API
     */
    public List<ThreadsPost> retrieveAndStoreUserPosts(String userId, String accessToken) {
        try {
            String url = UriComponentsBuilder.fromUriString(THREADS_API_BASE_URL + "/me/threads")
                    .queryParam("fields",
                            "id,media_product_type,media_type,media_url,permalink,owner,username,text,timestamp,shortcode,thumbnail_url,children,is_quote_post,has_replies,is_reply,replied_to")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            logger.info("Fetching user posts from: {}", url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode dataArray = jsonResponse.get("data");

                List<ThreadsPost> posts = new ArrayList<>();
                if (dataArray != null && dataArray.isArray()) {
                    for (JsonNode postNode : dataArray) {
                        ThreadsPost post = parsePostFromJson(postNode);
                        if (post != null) {
                            // Save or update post
                            ThreadsPost savedPost = saveOrUpdatePost(post);
                            posts.add(savedPost);
                        }
                    }
                }

                logger.info("Retrieved and stored {} posts for user {}", posts.size(), userId);
                return posts;
            }
        } catch (Exception e) {
            logger.error("Error retrieving user posts for user {}: {}", userId, e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Get posts with insights data
     */
    public List<ThreadsPost> getPostsWithInsights(String userId, String accessToken) {
        List<ThreadsPost> posts = retrieveAndStoreUserPosts(userId, accessToken);

        // Fetch insights for each post
        for (ThreadsPost post : posts) {
            try {
                updatePostInsights(post.getId(), accessToken);
            } catch (Exception e) {
                logger.warn("Failed to update insights for post {}: {}", post.getId(), e.getMessage());
            }
        }

        return posts;
    }

    /**
     * Update post insights from Threads API
     */
    public void updatePostInsights(String postId, String accessToken) {
        try {
            String url = UriComponentsBuilder.fromUriString(THREADS_API_BASE_URL + "/" + postId + "/insights")
                    .queryParam("metric", "views,likes,replies,reposts,quotes")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode dataArray = jsonResponse.get("data");

                Optional<ThreadsPost> postOpt = postsRepository.findById(postId);
                if (postOpt.isPresent()) {
                    ThreadsPost post = postOpt.get();

                    if (dataArray != null && dataArray.isArray()) {
                        for (JsonNode metricNode : dataArray) {
                            String metricName = metricNode.get("name").asText();
                            JsonNode valuesArray = metricNode.get("values");

                            if (valuesArray != null && valuesArray.isArray() && valuesArray.size() > 0) {
                                long value = valuesArray.get(0).get("value").asLong();

                                switch (metricName) {
                                    case "views":
                                        post.setViewsCount(value);
                                        break;
                                    case "likes":
                                        post.setLikesCount(value);
                                        break;
                                    case "replies":
                                        post.setRepliesCount(value);
                                        break;
                                    case "reposts":
                                        post.setRepostsCount(value);
                                        break;
                                    case "quotes":
                                        post.setQuotesCount(value);
                                        break;
                                }
                            }
                        }
                    }

                    postsRepository.save(post);
                    logger.info("Updated insights for post {}", postId);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating insights for post {}: {}", postId, e.getMessage());
        }
    }

    /**
     * Delete a post via Threads API and mark as deleted in database
     */
    public boolean deletePost(String postId, String accessToken) {
        try {
            String url = UriComponentsBuilder.fromUriString(THREADS_API_BASE_URL + "/" + postId)
                    .queryParam("access_token", accessToken)
                    .toUriString();

            restTemplate.delete(url);

            // Mark as deleted in database
            Optional<ThreadsPost> postOpt = postsRepository.findById(postId);
            if (postOpt.isPresent()) {
                ThreadsPost post = postOpt.get();
                post.setIsDeleted(true);
                postsRepository.save(post);
                logger.info("Marked post {} as deleted", postId);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error deleting post {}: {}", postId, e.getMessage());
        }
        return false;
    }

    /**
     * Get user's posts from database
     */
    public List<ThreadsPost> getUserPosts(String userId) {
        return postsRepository.findByUserIdAndIsDeletedFalseOrderByTimestampDesc(userId);
    }

    /**
     * Get user's posts with pagination
     */
    public Page<ThreadsPost> getUserPosts(String userId, Pageable pageable) {
        return postsRepository.findByUserIdAndIsDeletedFalse(userId, pageable);
    }

    /**
     * Get post by ID
     */
    public Optional<ThreadsPost> getPostById(String postId) {
        return postsRepository.findById(postId);
    }

    /**
     * Get top posts by views
     */
    public Page<ThreadsPost> getTopPostsByViews(Pageable pageable) {
        return postsRepository.findTopPostsByViews(pageable);
    }

    /**
     * Get top posts by likes
     */
    public Page<ThreadsPost> getTopPostsByLikes(Pageable pageable) {
        return postsRepository.findTopPostsByLikes(pageable);
    }

    /**
     * Get posts within date range
     */
    public List<ThreadsPost> getPostsInDateRange(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        return postsRepository.findPostsByUserInDateRange(userId, startDate, endDate);
    }

    /**
     * Get user statistics
     */
    public PostStatistics getUserStatistics(String userId) {
        Long totalPosts = postsRepository.countPostsByUser(userId);
        Long totalViews = postsRepository.getTotalViewsByUser(userId);
        Long totalLikes = postsRepository.getTotalLikesByUser(userId);

        // Handle null values from repository (in case of no data)
        totalPosts = totalPosts != null ? totalPosts : 0L;
        totalViews = totalViews != null ? totalViews : 0L;
        totalLikes = totalLikes != null ? totalLikes : 0L;

        return new PostStatistics(totalPosts, totalViews, totalLikes);
    }

    /**
     * Save or update post in database
     */
    private ThreadsPost saveOrUpdatePost(ThreadsPost post) {
        Optional<ThreadsPost> existingPost = postsRepository.findById(post.getId());
        if (existingPost.isPresent()) {
            ThreadsPost existing = existingPost.get();
            // Update existing post with new data
            existing.setText(post.getText());
            existing.setMediaType(post.getMediaType());
            existing.setMediaUrl(post.getMediaUrl());
            existing.setThumbnailUrl(post.getThumbnailUrl());
            existing.setHasReplies(post.getHasReplies());
            existing.setIsQuotePost(post.getIsQuotePost());
            existing.setIsReply(post.getIsReply());
            existing.setRepliedToId(post.getRepliedToId());
            existing.setRootPostId(post.getRootPostId());
            return postsRepository.save(existing);
        } else {
            return postsRepository.save(post);
        }
    }

    /**
     * Parse Threads API timestamp format to LocalDateTime
     */
    private LocalDateTime parseThreadsTimestamp(String timestamp) {
        try {
            // Parse as OffsetDateTime first, then convert to LocalDateTime in UTC
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp, THREADS_TIMESTAMP_FORMATTER);
            return offsetDateTime.toLocalDateTime();
        } catch (Exception e) {
            logger.warn("Failed to parse timestamp '{}', trying alternative format", timestamp);
            try {
                // Fallback to ISO format
                return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (Exception e2) {
                logger.error("Failed to parse timestamp '{}' with both formatters", timestamp);
                return LocalDateTime.now(); // Fallback to current time
            }
        }
    }

    /**
     * Parse post from JSON response
     */
    private ThreadsPost parsePostFromJson(JsonNode postNode) {
        try {
            String id = postNode.get("id").asText();

            ThreadsPost post = new ThreadsPost();
            post.setId(id);

            // Owner information
            JsonNode ownerNode = postNode.get("owner");
            if (ownerNode != null) {
                post.setUserId(ownerNode.get("id").asText());
            }

            if (postNode.has("username")) {
                post.setUsername(postNode.get("username").asText());
            }

            if (postNode.has("text")) {
                post.setText(postNode.get("text").asText());
            }

            if (postNode.has("media_type")) {
                String mediaType = postNode.get("media_type").asText();
                post.setMediaType(parseMediaType(mediaType));
            }

            if (postNode.has("media_url")) {
                post.setMediaUrl(postNode.get("media_url").asText());
            }

            if (postNode.has("permalink")) {
                post.setPermalink(postNode.get("permalink").asText());
            }

            if (postNode.has("shortcode")) {
                post.setShortcode(postNode.get("shortcode").asText());
            }

            if (postNode.has("thumbnail_url")) {
                post.setThumbnailUrl(postNode.get("thumbnail_url").asText());
            }

            if (postNode.has("timestamp")) {
                String timestamp = postNode.get("timestamp").asText();
                post.setTimestamp(parseThreadsTimestamp(timestamp));
            }

            if (postNode.has("has_replies")) {
                post.setHasReplies(postNode.get("has_replies").asBoolean());
            }

            if (postNode.has("is_quote_post")) {
                post.setIsQuotePost(postNode.get("is_quote_post").asBoolean());
            }

            if (postNode.has("is_reply")) {
                post.setIsReply(postNode.get("is_reply").asBoolean());
            }

            // Handle replied_to relationship
            JsonNode repliedToNode = postNode.get("replied_to");
            if (repliedToNode != null) {
                post.setRepliedToId(repliedToNode.get("id").asText());
            }

            return post;
        } catch (Exception e) {
            logger.error("Error parsing post from JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parse media type from string
     */
    private ThreadsPost.MediaType parseMediaType(String mediaType) {
        switch (mediaType.toUpperCase()) {
            case "TEXT_POST":
                return ThreadsPost.MediaType.TEXT_POST;
            case "IMAGE":
                return ThreadsPost.MediaType.IMAGE;
            case "VIDEO":
                return ThreadsPost.MediaType.VIDEO;
            case "CAROUSEL_ALBUM":
                return ThreadsPost.MediaType.CAROUSEL_ALBUM;
            default:
                return ThreadsPost.MediaType.TEXT_POST;
        }
    }

    /**
     * Post statistics DTO
     */
    public static class PostStatistics {
        private final Long totalPosts;
        private final Long totalViews;
        private final Long totalLikes;

        public PostStatistics(Long totalPosts, Long totalViews, Long totalLikes) {
            this.totalPosts = totalPosts != null ? totalPosts : 0L;
            this.totalViews = totalViews != null ? totalViews : 0L;
            this.totalLikes = totalLikes != null ? totalLikes : 0L;
        }

        public Long getTotalPosts() {
            return totalPosts;
        }

        public Long getTotalViews() {
            return totalViews;
        }

        public Long getTotalLikes() {
            return totalLikes;
        }
    }
}