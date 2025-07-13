package com.tadeasfort.threadsapi.controller;

import com.tadeasfort.threadsapi.entity.SearchResult;
import com.tadeasfort.threadsapi.service.ThreadsSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@Tag(name = "Search", description = "Keyword search functionality with caching and analytics")
public class SearchController {

    @Autowired
    private ThreadsSearchService searchService;

    @GetMapping("/posts")
    @Operation(summary = "Search posts by keyword", description = "Search for posts using keywords with optional caching")
    public ResponseEntity<List<SearchResult>> searchPosts(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Search type (TOP or RECENT)") @RequestParam(defaultValue = "TOP") String searchType,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Access token") @RequestParam String accessToken,
            @Parameter(description = "Use cached results") @RequestParam(defaultValue = "true") boolean useCache) {

        List<SearchResult> results = searchService.searchPosts(query, searchType, userId, accessToken, useCache);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/fresh")
    @Operation(summary = "Perform fresh search", description = "Perform a fresh search without using cache")
    public ResponseEntity<List<SearchResult>> performFreshSearch(
            @Parameter(description = "Search query") @RequestParam String query,
            @Parameter(description = "Search type (TOP or RECENT)") @RequestParam(defaultValue = "TOP") String searchType,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Access token") @RequestParam String accessToken) {

        List<SearchResult> results = searchService.performFreshSearch(query, searchType, userId, accessToken);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get user search history", description = "Retrieve search history for a specific user")
    public ResponseEntity<List<String>> getUserSearchHistory(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Limit number of results") @RequestParam(defaultValue = "20") int limit) {

        List<String> history = searchService.getUserSearchHistory(userId, limit);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular search queries", description = "Retrieve most popular search queries")
    public ResponseEntity<List<ThreadsSearchService.PopularQuery>> getPopularQueries(
            @Parameter(description = "Limit number of results") @RequestParam(defaultValue = "10") int limit) {

        List<ThreadsSearchService.PopularQuery> popularQueries = searchService.getPopularQueries(limit);
        return ResponseEntity.ok(popularQueries);
    }

    @GetMapping("/results")
    @Operation(summary = "Get search results by query", description = "Retrieve cached search results for a specific query")
    public ResponseEntity<List<SearchResult>> getSearchResultsByQuery(
            @Parameter(description = "Search query") @RequestParam String query) {

        List<SearchResult> results = searchService.getSearchResultsByQuery(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/user/{userId}/results")
    @Operation(summary = "Get user's search results", description = "Retrieve all search results for a specific user")
    public ResponseEntity<List<SearchResult>> getUserSearchResults(
            @Parameter(description = "User ID") @PathVariable String userId) {

        List<SearchResult> results = searchService.getUserSearchResults(userId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get search analytics", description = "Get search analytics for a date range")
    public ResponseEntity<ThreadsSearchService.SearchAnalytics> getSearchAnalytics(
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        ThreadsSearchService.SearchAnalytics analytics = searchService.getSearchAnalytics(userId, startDate, endDate);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/recent-keywords")
    @Operation(summary = "Get recently searched keywords", description = "Get recently searched keywords from Threads API")
    public ResponseEntity<List<ThreadsSearchService.RecentKeyword>> getRecentlySearchedKeywords(
            @Parameter(description = "Access token") @RequestParam String accessToken) {

        List<ThreadsSearchService.RecentKeyword> keywords = searchService.getRecentlySearchedKeywords(accessToken);
        return ResponseEntity.ok(keywords);
    }

    @DeleteMapping("/cleanup")
    @Operation(summary = "Cleanup old search results", description = "Remove old search results from cache")
    public ResponseEntity<String> cleanupOldSearchResults(
            @Parameter(description = "Days to keep") @RequestParam(defaultValue = "30") int daysToKeep) {

        searchService.cleanupOldSearchResults(daysToKeep);
        return ResponseEntity.ok("Search results cleanup completed");
    }
}