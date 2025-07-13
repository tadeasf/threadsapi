package com.tadeasfort.threadsapi.service;

import com.tadeasfort.threadsapi.entity.ThreadsInsight;
import com.tadeasfort.threadsapi.entity.ThreadsPost;
import com.tadeasfort.threadsapi.repository.ThreadsInsightRepository;
import com.tadeasfort.threadsapi.repository.ThreadsPostRepository;
import com.tadeasfort.threadsapi.aspect.RateLimitAspect.RateLimit;
import com.tadeasfort.threadsapi.aspect.RateLimitAspect.RateLimitType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ThreadsInsightsService {

    private static final Logger logger = LoggerFactory.getLogger(ThreadsInsightsService.class);
    private static final String THREADS_API_BASE_URL = "https://graph.threads.net/v1.0";

    // Custom formatter for Threads API timestamps (e.g.,
    // "2025-06-22T13:00:27+0000")
    private static final DateTimeFormatter THREADS_TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Autowired
    private ThreadsInsightRepository insightsRepository;

    @Autowired
    private ThreadsPostRepository postsRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Fetch and store user insights from Threads API
     * Available metrics: views, likes, replies, quotes, clicks, followers_count
     */
    @RateLimit(type = RateLimitType.API_CALL, userIdParamIndex = 0)
    public List<ThreadsInsight> fetchAndStoreUserInsights(String userId, String accessToken) {
        try {
            String url = UriComponentsBuilder.fromUriString(THREADS_API_BASE_URL + "/me/threads_insights")
                    .queryParam("metric", "views,likes,replies,quotes,clicks,followers_count")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            logger.info("Fetching user insights for user: {}", userId);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode dataArray = jsonResponse.get("data");

                List<ThreadsInsight> insights = new ArrayList<>();
                if (dataArray != null && dataArray.isArray()) {
                    for (JsonNode insightNode : dataArray) {
                        List<ThreadsInsight> parsedInsights = parseUserInsightFromJson(insightNode, userId);
                        for (ThreadsInsight insight : parsedInsights) {
                            ThreadsInsight savedInsight = insightsRepository.save(insight);
                            insights.add(savedInsight);
                        }
                    }
                }

                logger.info("Fetched and stored {} user insights for user {}", insights.size(), userId);
                return insights;
            }
        } catch (Exception e) {
            logger.error("Error fetching user insights for user {}: {}", userId, e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Fetch and store media insights for a specific post
     * Available metrics: views, likes, replies, reposts, quotes, shares
     */
    @RateLimit(type = RateLimitType.API_CALL, userIdParamIndex = 1)
    public List<ThreadsInsight> fetchAndStoreMediaInsights(String postId, String userId, String accessToken) {
        try {
            String url = UriComponentsBuilder.fromUriString(THREADS_API_BASE_URL + "/" + postId + "/insights")
                    .queryParam("metric", "views,likes,replies,reposts,quotes,shares")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            logger.info("Fetching media insights for post: {}", postId);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode dataArray = jsonResponse.get("data");

                List<ThreadsInsight> insights = new ArrayList<>();
                if (dataArray != null && dataArray.isArray()) {
                    for (JsonNode insightNode : dataArray) {
                        ThreadsInsight insight = parseMediaInsightFromJson(insightNode, postId, userId);
                        if (insight != null) {
                            ThreadsInsight savedInsight = insightsRepository.save(insight);
                            insights.add(savedInsight);
                        }
                    }
                }

                logger.info("Fetched and stored {} media insights for post {}", insights.size(), postId);
                return insights;
            }
        } catch (Exception e) {
            logger.error("Error fetching media insights for post {}: {}", postId, e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Get comprehensive insights dashboard with all available metrics
     */
    public InsightsDashboard getInsightsDashboard(String userId) {
        // Get latest user insights
        List<ThreadsInsight> userInsights = insightsRepository.findLatestUserInsights(userId);
        Map<String, Long> userMetrics = userInsights.stream()
                .collect(Collectors.toMap(
                        ThreadsInsight::getMetricName,
                        ThreadsInsight::getMetricValue,
                        (existing, replacement) -> replacement));

        // Get posts with insights for media metrics
        List<ThreadsPost> posts = postsRepository.findByUserIdAndIsDeletedFalseOrderByTimestampDesc(userId);

        // Calculate total media metrics across all posts
        Map<String, Long> totalMediaMetrics = new HashMap<>();
        totalMediaMetrics.put("total_views",
                posts.stream().mapToLong(p -> p.getViewsCount() != null ? p.getViewsCount() : 0L).sum());
        totalMediaMetrics.put("total_likes",
                posts.stream().mapToLong(p -> p.getLikesCount() != null ? p.getLikesCount() : 0L).sum());
        totalMediaMetrics.put("total_replies",
                posts.stream().mapToLong(p -> p.getRepliesCount() != null ? p.getRepliesCount() : 0L).sum());
        totalMediaMetrics.put("total_reposts",
                posts.stream().mapToLong(p -> p.getRepostsCount() != null ? p.getRepostsCount() : 0L).sum());
        totalMediaMetrics.put("total_quotes",
                posts.stream().mapToLong(p -> p.getQuotesCount() != null ? p.getQuotesCount() : 0L).sum());

        // Calculate average engagement per post
        int postCount = posts.size();
        Map<String, Double> averageMetrics = new HashMap<>();
        if (postCount > 0) {
            averageMetrics.put("avg_views", totalMediaMetrics.get("total_views").doubleValue() / postCount);
            averageMetrics.put("avg_likes", totalMediaMetrics.get("total_likes").doubleValue() / postCount);
            averageMetrics.put("avg_replies", totalMediaMetrics.get("total_replies").doubleValue() / postCount);
            averageMetrics.put("avg_reposts", totalMediaMetrics.get("total_reposts").doubleValue() / postCount);
            averageMetrics.put("avg_quotes", totalMediaMetrics.get("total_quotes").doubleValue() / postCount);
        }

        // Get top performing posts (sorted by total engagement)
        List<TopPost> topPosts = posts.stream()
                .sorted((p1, p2) -> {
                    // Calculate total engagement for each post
                    long engagement1 = (p1.getViewsCount() != null ? p1.getViewsCount() : 0L) +
                            (p1.getLikesCount() != null ? p1.getLikesCount() : 0L) +
                            (p1.getRepliesCount() != null ? p1.getRepliesCount() : 0L) +
                            (p1.getRepostsCount() != null ? p1.getRepostsCount() : 0L) +
                            (p1.getQuotesCount() != null ? p1.getQuotesCount() : 0L);

                    long engagement2 = (p2.getViewsCount() != null ? p2.getViewsCount() : 0L) +
                            (p2.getLikesCount() != null ? p2.getLikesCount() : 0L) +
                            (p2.getRepliesCount() != null ? p2.getRepliesCount() : 0L) +
                            (p2.getRepostsCount() != null ? p2.getRepostsCount() : 0L) +
                            (p2.getQuotesCount() != null ? p2.getQuotesCount() : 0L);

                    // Sort by total engagement descending
                    return Long.compare(engagement2, engagement1);
                })
                .limit(10) // Get top 10 instead of 5 for better selection
                .map(post -> new TopPost(
                        post.getId(),
                        post.getText() != null ? post.getText() : "",
                        post.getViewsCount() != null ? post.getViewsCount() : 0L,
                        post.getLikesCount() != null ? post.getLikesCount() : 0L,
                        post.getRepliesCount() != null ? post.getRepliesCount() : 0L,
                        post.getTimestamp()))
                .collect(Collectors.toList());

        // Get engagement trends over time (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<ThreadsInsight> recentInsights = insightsRepository.findInsightsInDateRange(userId, thirtyDaysAgo,
                LocalDateTime.now());

        Map<LocalDateTime, Map<String, Long>> dailyMetrics = new HashMap<>();
        for (ThreadsInsight insight : recentInsights) {
            LocalDateTime date = insight.getDateRecorded().toLocalDate().atStartOfDay();
            dailyMetrics.computeIfAbsent(date, k -> new HashMap<>())
                    .put(insight.getMetricName(), insight.getMetricValue());
        }

        List<EngagementTrend> trends = dailyMetrics.entrySet().stream()
                .map(entry -> new EngagementTrend(
                        entry.getKey(),
                        entry.getValue().getOrDefault("views", 0L),
                        entry.getValue().getOrDefault("likes", 0L),
                        entry.getValue().getOrDefault("replies", 0L),
                        entry.getValue().getOrDefault("reposts", 0L),
                        entry.getValue().getOrDefault("quotes", 0L)))
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());

        return new InsightsDashboard(userMetrics, totalMediaMetrics, averageMetrics, topPosts, trends, postCount);
    }

    /**
     * Get post performance analytics for a specific time period
     */
    public PostPerformanceAnalytics getPostPerformanceAnalytics(String userId, LocalDateTime startDate,
            LocalDateTime endDate) {
        List<ThreadsPost> posts = postsRepository.findPostsByUserInDateRange(userId, startDate, endDate);

        Map<String, List<Long>> metricsByType = new HashMap<>();
        metricsByType.put("views", new ArrayList<>());
        metricsByType.put("likes", new ArrayList<>());
        metricsByType.put("replies", new ArrayList<>());
        metricsByType.put("reposts", new ArrayList<>());
        metricsByType.put("quotes", new ArrayList<>());

        for (ThreadsPost post : posts) {
            metricsByType.get("views").add(post.getViewsCount() != null ? post.getViewsCount() : 0L);
            metricsByType.get("likes").add(post.getLikesCount() != null ? post.getLikesCount() : 0L);
            metricsByType.get("replies").add(post.getRepliesCount() != null ? post.getRepliesCount() : 0L);
            metricsByType.get("reposts").add(post.getRepostsCount() != null ? post.getRepostsCount() : 0L);
            metricsByType.get("quotes").add(post.getQuotesCount() != null ? post.getQuotesCount() : 0L);
        }

        Map<String, MetricSummary> summaries = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : metricsByType.entrySet()) {
            String metric = entry.getKey();
            List<Long> values = entry.getValue();

            if (!values.isEmpty()) {
                long total = values.stream().mapToLong(Long::longValue).sum();
                double average = values.stream().mapToLong(Long::longValue).average().orElse(0.0);
                long max = values.stream().mapToLong(Long::longValue).max().orElse(0L);
                long min = values.stream().mapToLong(Long::longValue).min().orElse(0L);

                summaries.put(metric, new MetricSummary(total, average, max, min, values.size()));
            }
        }

        return new PostPerformanceAnalytics(summaries, startDate, endDate);
    }

    /**
     * Get engagement trends over time
     */
    public List<EngagementTrend> getEngagementTrends(String userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<ThreadsInsight> insights = insightsRepository.findInsightsInDateRange(userId, startDate,
                LocalDateTime.now());

        Map<LocalDateTime, Map<String, Long>> dailyMetrics = new HashMap<>();

        for (ThreadsInsight insight : insights) {
            LocalDateTime date = insight.getDateRecorded().toLocalDate().atStartOfDay();
            dailyMetrics.computeIfAbsent(date, k -> new HashMap<>())
                    .put(insight.getMetricName(), insight.getMetricValue());
        }

        List<EngagementTrend> trends = new ArrayList<>();
        for (Map.Entry<LocalDateTime, Map<String, Long>> entry : dailyMetrics.entrySet()) {
            LocalDateTime date = entry.getKey();
            Map<String, Long> metrics = entry.getValue();

            trends.add(new EngagementTrend(date,
                    metrics.getOrDefault("views", 0L),
                    metrics.getOrDefault("likes", 0L),
                    metrics.getOrDefault("replies", 0L),
                    metrics.getOrDefault("reposts", 0L),
                    metrics.getOrDefault("quotes", 0L)));
        }

        trends.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return trends;
    }

    /**
     * Parse Threads API timestamp format to LocalDateTime
     */
    private LocalDateTime parseThreadsTimestamp(String timestamp) {
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp, THREADS_TIMESTAMP_FORMATTER);
            return offsetDateTime.toLocalDateTime();
        } catch (Exception e) {
            logger.warn("Failed to parse timestamp: {}, using current time", timestamp);
            return LocalDateTime.now();
        }
    }

    /**
     * Parse user insight from JSON response
     */
    private List<ThreadsInsight> parseUserInsightFromJson(JsonNode insightNode, String userId) {
        List<ThreadsInsight> insights = new ArrayList<>();

        try {
            String metricName = insightNode.get("name").asText();
            String period = insightNode.has("period") ? insightNode.get("period").asText() : "lifetime";

            JsonNode valuesArray = insightNode.get("values");
            if (valuesArray != null && valuesArray.isArray()) {
                for (JsonNode valueNode : valuesArray) {
                    ThreadsInsight insight = new ThreadsInsight(userId, ThreadsInsight.InsightType.USER_INSIGHT,
                            metricName, 0L);
                    insight.setPeriod(period);

                    if (valueNode.has("value")) {
                        insight.setMetricValue(valueNode.get("value").asLong());
                    }

                    if (valueNode.has("end_time")) {
                        String endTime = valueNode.get("end_time").asText();
                        insight.setDateRecorded(parseThreadsTimestamp(endTime));
                    }

                    insights.add(insight);
                }
            }

            // Handle demographic breakdowns for followers_count
            if (insightNode.has("total_value") && insightNode.get("total_value").has("breakdowns")) {
                JsonNode breakdowns = insightNode.get("total_value").get("breakdowns");
                ThreadsInsight demographicInsight = new ThreadsInsight(userId, ThreadsInsight.InsightType.USER_INSIGHT,
                        metricName + "_demographics", 0L);
                demographicInsight.setBreakdownData(breakdowns.toString());
                insights.add(demographicInsight);
            }

        } catch (Exception e) {
            logger.error("Error parsing user insight from JSON: {}", e.getMessage());
        }

        return insights;
    }

    /**
     * Parse media insight from JSON response
     */
    private ThreadsInsight parseMediaInsightFromJson(JsonNode insightNode, String postId, String userId) {
        try {
            String metricName = insightNode.get("name").asText();
            String period = insightNode.has("period") ? insightNode.get("period").asText() : "lifetime";

            ThreadsInsight insight = new ThreadsInsight(userId, ThreadsInsight.InsightType.MEDIA_INSIGHT, metricName,
                    0L);
            insight.setPostId(postId);
            insight.setPeriod(period);

            JsonNode valuesArray = insightNode.get("values");
            if (valuesArray != null && valuesArray.isArray() && valuesArray.size() > 0) {
                JsonNode valueNode = valuesArray.get(0);
                if (valueNode.has("value")) {
                    insight.setMetricValue(valueNode.get("value").asLong());
                }
            }

            return insight;
        } catch (Exception e) {
            logger.error("Error parsing media insight from JSON: {}", e.getMessage());
            return null;
        }
    }

    // DTOs for enhanced insights dashboard

    /**
     * Comprehensive insights dashboard DTO
     */
    public static class InsightsDashboard {
        private final Map<String, Long> userMetrics;
        private final Map<String, Long> totalMediaMetrics;
        private final Map<String, Double> averageMetrics;
        private final List<TopPost> topPosts;
        private final List<EngagementTrend> engagementTrends;
        private final int totalPosts;

        public InsightsDashboard(Map<String, Long> userMetrics, Map<String, Long> totalMediaMetrics,
                Map<String, Double> averageMetrics, List<TopPost> topPosts,
                List<EngagementTrend> engagementTrends, int totalPosts) {
            this.userMetrics = userMetrics;
            this.totalMediaMetrics = totalMediaMetrics;
            this.averageMetrics = averageMetrics;
            this.topPosts = topPosts;
            this.engagementTrends = engagementTrends;
            this.totalPosts = totalPosts;
        }

        // Getters
        public Map<String, Long> getUserMetrics() {
            return userMetrics;
        }

        public Map<String, Long> getTotalMediaMetrics() {
            return totalMediaMetrics;
        }

        public Map<String, Double> getAverageMetrics() {
            return averageMetrics;
        }

        public List<TopPost> getTopPosts() {
            return topPosts;
        }

        public List<EngagementTrend> getEngagementTrends() {
            return engagementTrends;
        }

        public int getTotalPosts() {
            return totalPosts;
        }
    }

    /**
     * Top performing post DTO
     */
    public static class TopPost {
        private final String id;
        private final String text;
        private final Long views;
        private final Long likes;
        private final Long replies;
        private final LocalDateTime timestamp;

        public TopPost(String id, String text, Long views, Long likes, Long replies, LocalDateTime timestamp) {
            this.id = id;
            this.text = text;
            this.views = views;
            this.likes = likes;
            this.replies = replies;
            this.timestamp = timestamp;
        }

        // Getters
        public String getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public Long getViews() {
            return views;
        }

        public Long getLikes() {
            return likes;
        }

        public Long getReplies() {
            return replies;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Daily metric DTO
     */
    public static class DailyMetric {
        private final LocalDateTime date;
        private final Long value;

        public DailyMetric(LocalDateTime date, Long value) {
            this.date = date;
            this.value = value;
        }

        public LocalDateTime getDate() {
            return date;
        }

        public Long getValue() {
            return value;
        }
    }

    /**
     * Post performance analytics DTO
     */
    public static class PostPerformanceAnalytics {
        private final Map<String, MetricSummary> metricSummaries;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;

        public PostPerformanceAnalytics(Map<String, MetricSummary> metricSummaries, LocalDateTime startDate,
                LocalDateTime endDate) {
            this.metricSummaries = metricSummaries;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public Map<String, MetricSummary> getMetricSummaries() {
            return metricSummaries;
        }

        public LocalDateTime getStartDate() {
            return startDate;
        }

        public LocalDateTime getEndDate() {
            return endDate;
        }
    }

    /**
     * Metric summary DTO
     */
    public static class MetricSummary {
        private final long total;
        private final double average;
        private final long max;
        private final long min;
        private final int count;

        public MetricSummary(long total, double average, long max, long min, int count) {
            this.total = total;
            this.average = average;
            this.max = max;
            this.min = min;
            this.count = count;
        }

        public long getTotal() {
            return total;
        }

        public double getAverage() {
            return average;
        }

        public long getMax() {
            return max;
        }

        public long getMin() {
            return min;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * Engagement trend data DTO
     */
    public static class EngagementTrend {
        private final LocalDateTime date;
        private final Long views;
        private final Long likes;
        private final Long replies;
        private final Long reposts;
        private final Long quotes;

        public EngagementTrend(LocalDateTime date, Long views, Long likes, Long replies, Long reposts, Long quotes) {
            this.date = date;
            this.views = views;
            this.likes = likes;
            this.replies = replies;
            this.reposts = reposts;
            this.quotes = quotes;
        }

        public LocalDateTime getDate() {
            return date;
        }

        public Long getViews() {
            return views;
        }

        public Long getLikes() {
            return likes;
        }

        public Long getReplies() {
            return replies;
        }

        public Long getReposts() {
            return reposts;
        }

        public Long getQuotes() {
            return quotes;
        }
    }
}