package com.tadeasfort.threadsapi.service;

import com.tadeasfort.threadsapi.entity.SearchResult;
import com.tadeasfort.threadsapi.entity.ThreadsPost;
import com.tadeasfort.threadsapi.repository.SearchResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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

@Service
@Transactional
public class ThreadsSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ThreadsSearchService.class);
    private static final String THREADS_API_BASE_URL = "https://graph.threads.net/v1.0";
    private static final int CACHE_HOURS = 1; // Cache results for 1 hour

    // Custom formatter for Threads API timestamps (e.g.,
    // "2025-06-22T13:00:27+0000")
    private static final DateTimeFormatter THREADS_TIMESTAMP_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Autowired
    private SearchResultRepository searchResultRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Search for posts by keyword with caching
     */
    public List<SearchResult> searchPosts(String query, String searchType, String userId, String accessToken,
            boolean useCache) {
        // Check cache first if enabled
        if (useCache) {
            LocalDateTime cacheThreshold = LocalDateTime.now().minusHours(CACHE_HOURS);
            List<SearchResult> cachedResults = searchResultRepository.findRecentSearchResults(query, cacheThreshold);

            if (!cachedResults.isEmpty()) {
                logger.info("Returning cached search results for query: {}", query);
                return cachedResults;
            }
        }

        // Perform fresh search via API
        return performFreshSearch(query, searchType, userId, accessToken);
    }

    /**
     * Perform fresh search via Threads API
     */
    public List<SearchResult> performFreshSearch(String query, String searchType, String userId, String accessToken) {
        try {
            String url = UriComponentsBuilder.fromUriString(THREADS_API_BASE_URL + "/keyword_search")
                    .queryParam("q", query)
                    .queryParam("search_type", searchType != null ? searchType : "TOP")
                    .queryParam("fields",
                            "id,text,media_type,permalink,timestamp,username,has_replies,is_quote_post,is_reply")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            logger.info("Performing fresh search for query: {}", query);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode dataArray = jsonResponse.get("data");

                List<SearchResult> results = new ArrayList<>();
                if (dataArray != null && dataArray.isArray()) {
                    for (JsonNode resultNode : dataArray) {
                        SearchResult searchResult = parseSearchResultFromJson(resultNode, query, searchType, userId);
                        if (searchResult != null) {
                            SearchResult savedResult = searchResultRepository.save(searchResult);
                            results.add(savedResult);
                        }
                    }
                }

                logger.info("Found and cached {} search results for query: {}", results.size(), query);
                return results;
            }
        } catch (Exception e) {
            logger.error("Error performing search for query {}: {}", query, e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Get user's search history
     */
    public List<String> getUserSearchHistory(String userId, int limit) {
        return searchResultRepository.findUserSearchHistory(userId, PageRequest.of(0, limit));
    }

    /**
     * Get popular search queries
     */
    public List<PopularQuery> getPopularQueries(int limit) {
        List<Object[]> results = searchResultRepository.findPopularQueries(PageRequest.of(0, limit));
        List<PopularQuery> popularQueries = new ArrayList<>();

        for (Object[] result : results) {
            String query = (String) result[0];
            Long count = (Long) result[1];
            popularQueries.add(new PopularQuery(query, count));
        }

        return popularQueries;
    }

    /**
     * Get search results by query
     */
    public List<SearchResult> getSearchResultsByQuery(String query) {
        return searchResultRepository.findByQueryOrderBySearchTimestampDesc(query);
    }

    /**
     * Get user's search results
     */
    public List<SearchResult> getUserSearchResults(String userId) {
        return searchResultRepository.findByUserIdOrderBySearchTimestampDesc(userId);
    }

    /**
     * Get search analytics for a date range
     */
    public SearchAnalytics getSearchAnalytics(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<SearchResult> results = searchResultRepository.findSearchResultsInDateRange(startDate, endDate);

        long totalSearches = results.size();
        long uniqueQueries = results.stream()
                .map(SearchResult::getQuery)
                .distinct()
                .count();

        // Calculate average results per search
        double avgResultsPerSearch = results.isEmpty() ? 0.0
                : results.stream()
                        .collect(java.util.stream.Collectors.groupingBy(SearchResult::getQuery))
                        .values()
                        .stream()
                        .mapToInt(List::size)
                        .average()
                        .orElse(0.0);

        return new SearchAnalytics(totalSearches, uniqueQueries, avgResultsPerSearch, startDate, endDate);
    }

    /**
     * Clean up old search results
     */
    public void cleanupOldSearchResults(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        searchResultRepository.deleteBySearchTimestampBefore(cutoffDate);
        logger.info("Cleaned up search results older than {} days", daysToKeep);
    }

    /**
     * Get recently searched keywords for current user
     */
    public List<RecentKeyword> getRecentlySearchedKeywords(String accessToken) {
        try {
            String url = UriComponentsBuilder.fromUriString(THREADS_API_BASE_URL + "/me")
                    .queryParam("fields", "recently_searched_keywords")
                    .queryParam("access_token", accessToken)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                JsonNode keywordsArray = jsonResponse.get("recently_searched_keywords");

                List<RecentKeyword> keywords = new ArrayList<>();
                if (keywordsArray != null && keywordsArray.isArray()) {
                    for (JsonNode keywordNode : keywordsArray) {
                        String query = keywordNode.get("query").asText();
                        long timestamp = keywordNode.get("timestamp").asLong();
                        LocalDateTime searchTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0,
                                java.time.ZoneOffset.UTC);
                        keywords.add(new RecentKeyword(query, searchTime));
                    }
                }

                return keywords;
            }
        } catch (Exception e) {
            logger.error("Error fetching recently searched keywords: {}", e.getMessage());
        }
        return new ArrayList<>();
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
     * Parse search result from JSON response
     */
    private SearchResult parseSearchResultFromJson(JsonNode resultNode, String query, String searchType,
            String userId) {
        try {
            SearchResult searchResult = new SearchResult(query, searchType, userId);

            if (resultNode.has("id")) {
                searchResult.setPostId(resultNode.get("id").asText());
            }

            if (resultNode.has("text")) {
                searchResult.setText(resultNode.get("text").asText());
            }

            if (resultNode.has("media_type")) {
                String mediaType = resultNode.get("media_type").asText();
                searchResult.setMediaType(parseMediaType(mediaType));
            }

            if (resultNode.has("permalink")) {
                searchResult.setPermalink(resultNode.get("permalink").asText());
            }

            if (resultNode.has("timestamp")) {
                String timestamp = resultNode.get("timestamp").asText();
                searchResult.setTimestamp(parseThreadsTimestamp(timestamp));
            }

            if (resultNode.has("username")) {
                searchResult.setUsername(resultNode.get("username").asText());
            }

            if (resultNode.has("has_replies")) {
                searchResult.setHasReplies(resultNode.get("has_replies").asBoolean());
            }

            if (resultNode.has("is_quote_post")) {
                searchResult.setIsQuotePost(resultNode.get("is_quote_post").asBoolean());
            }

            if (resultNode.has("is_reply")) {
                searchResult.setIsReply(resultNode.get("is_reply").asBoolean());
            }

            return searchResult;
        } catch (Exception e) {
            logger.error("Error parsing search result from JSON: {}", e.getMessage());
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
     * Popular query DTO
     */
    public static class PopularQuery {
        private final String query;
        private final Long count;

        public PopularQuery(String query, Long count) {
            this.query = query;
            this.count = count;
        }

        public String getQuery() {
            return query;
        }

        public Long getCount() {
            return count;
        }
    }

    /**
     * Recent keyword DTO
     */
    public static class RecentKeyword {
        private final String query;
        private final LocalDateTime timestamp;

        public RecentKeyword(String query, LocalDateTime timestamp) {
            this.query = query;
            this.timestamp = timestamp;
        }

        public String getQuery() {
            return query;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Search analytics DTO
     */
    public static class SearchAnalytics {
        private final long totalSearches;
        private final long uniqueQueries;
        private final double avgResultsPerSearch;
        private final LocalDateTime startDate;
        private final LocalDateTime endDate;

        public SearchAnalytics(long totalSearches, long uniqueQueries, double avgResultsPerSearch,
                LocalDateTime startDate, LocalDateTime endDate) {
            this.totalSearches = totalSearches;
            this.uniqueQueries = uniqueQueries;
            this.avgResultsPerSearch = avgResultsPerSearch;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public long getTotalSearches() {
            return totalSearches;
        }

        public long getUniqueQueries() {
            return uniqueQueries;
        }

        public double getAvgResultsPerSearch() {
            return avgResultsPerSearch;
        }

        public LocalDateTime getStartDate() {
            return startDate;
        }

        public LocalDateTime getEndDate() {
            return endDate;
        }
    }
}