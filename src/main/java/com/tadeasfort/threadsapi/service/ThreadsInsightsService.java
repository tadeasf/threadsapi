package com.tadeasfort.threadsapi.service;

import com.tadeasfort.threadsapi.entity.ThreadsInsight;
import com.tadeasfort.threadsapi.repository.ThreadsInsightRepository;
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
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Fetch and store user insights from Threads API
     */
    @RateLimit(type = RateLimitType.API_CALL, userIdParamIndex = 0)
    public List<ThreadsInsight> fetchAndStoreUserInsights(String userId, String accessToken) {
        try {
            String url = UriComponentsBuilder.fromUriString(THREADS_API_BASE_URL + "/me/threads_insights")
                    .queryParam("metric", "views,followers_count")
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
     */
    @RateLimit(type = RateLimitType.API_CALL, userIdParamIndex = 1)
    public List<ThreadsInsight> fetchAndStoreMediaInsights(String postId, String userId, String accessToken) {
        try {
            String url = UriComponentsBuilder.fromUriString(THREADS_API_BASE_URL + "/" + postId + "/insights")
                    .queryParam("metric", "views,likes,replies,reposts,quotes")
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
     * Get user insights dashboard data
     */
    public UserInsightsDashboard getUserInsightsDashboard(String userId) {
        List<ThreadsInsight> latestInsights = insightsRepository.findLatestUserInsights(userId);

        Map<String, Long> metrics = new HashMap<>();
        for (ThreadsInsight insight : latestInsights) {
            metrics.put(insight.getMetricName(), insight.getMetricValue());
        }

        // Get historical data for views (last 30 days)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<ThreadsInsight> viewsHistory = insightsRepository.findInsightsInDateRange(userId, thirtyDaysAgo,
                LocalDateTime.now());

        List<DailyMetric> dailyViews = new ArrayList<>();
        Map<LocalDateTime, Long> viewsByDate = new HashMap<>();

        for (ThreadsInsight insight : viewsHistory) {
            if ("views".equals(insight.getMetricName())) {
                LocalDateTime date = insight.getDateRecorded().toLocalDate().atStartOfDay();
                viewsByDate.put(date, insight.getMetricValue());
            }
        }

        for (Map.Entry<LocalDateTime, Long> entry : viewsByDate.entrySet()) {
            dailyViews.add(new DailyMetric(entry.getKey(), entry.getValue()));
        }

        return new UserInsightsDashboard(metrics, dailyViews);
    }

    /**
     * Get post performance analytics
     */
    public PostPerformanceAnalytics getPostPerformanceAnalytics(String userId, LocalDateTime startDate,
            LocalDateTime endDate) {
        List<ThreadsInsight> insights = insightsRepository.findInsightsInDateRange(userId, startDate, endDate);

        Map<String, List<Long>> metricsByType = new HashMap<>();
        metricsByType.put("views", new ArrayList<>());
        metricsByType.put("likes", new ArrayList<>());
        metricsByType.put("replies", new ArrayList<>());
        metricsByType.put("reposts", new ArrayList<>());
        metricsByType.put("quotes", new ArrayList<>());

        for (ThreadsInsight insight : insights) {
            if (insight.getInsightType() == ThreadsInsight.InsightType.MEDIA_INSIGHT) {
                String metricName = insight.getMetricName();
                if (metricsByType.containsKey(metricName)) {
                    metricsByType.get(metricName).add(insight.getMetricValue());
                }
            }
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

            // Handle demographic breakdowns
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

    /**
     * User insights dashboard DTO
     */
    public static class UserInsightsDashboard {
        private final Map<String, Long> currentMetrics;
        private final List<DailyMetric> dailyViews;

        public UserInsightsDashboard(Map<String, Long> currentMetrics, List<DailyMetric> dailyViews) {
            this.currentMetrics = currentMetrics;
            this.dailyViews = dailyViews;
        }

        public Map<String, Long> getCurrentMetrics() {
            return currentMetrics;
        }

        public List<DailyMetric> getDailyViews() {
            return dailyViews;
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
     * Engagement trend DTO
     */
    public static class EngagementTrend {
        private final LocalDateTime date;
        private final long views;
        private final long likes;
        private final long replies;
        private final long reposts;
        private final long quotes;

        public EngagementTrend(LocalDateTime date, long views, long likes, long replies, long reposts, long quotes) {
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

        public long getViews() {
            return views;
        }

        public long getLikes() {
            return likes;
        }

        public long getReplies() {
            return replies;
        }

        public long getReposts() {
            return reposts;
        }

        public long getQuotes() {
            return quotes;
        }
    }
}