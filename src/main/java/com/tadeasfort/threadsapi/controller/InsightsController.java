package com.tadeasfort.threadsapi.controller;

import com.tadeasfort.threadsapi.entity.ThreadsInsight;
import com.tadeasfort.threadsapi.service.ThreadsInsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/insights")
@Tag(name = "Insights", description = "Threads insights and analytics")
public class InsightsController {

    @Autowired
    private ThreadsInsightsService insightsService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user insights", description = "Fetch and store user insights from Threads API")
    public ResponseEntity<List<ThreadsInsight>> getUserInsights(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Access token") @RequestParam String accessToken) {

        List<ThreadsInsight> insights = insightsService.fetchAndStoreUserInsights(userId, accessToken);
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/post/{postId}")
    @Operation(summary = "Get post insights", description = "Fetch and store media insights for a specific post")
    public ResponseEntity<List<ThreadsInsight>> getPostInsights(
            @Parameter(description = "Post ID") @PathVariable String postId,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Access token") @RequestParam String accessToken) {

        List<ThreadsInsight> insights = insightsService.fetchAndStoreMediaInsights(postId, userId, accessToken);
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/dashboard/{userId}")
    @Operation(summary = "Get comprehensive insights dashboard", description = "Get comprehensive insights dashboard with all available metrics")
    public ResponseEntity<ThreadsInsightsService.InsightsDashboard> getInsightsDashboard(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Access token (optional)") @RequestParam(required = false) String accessToken) {

        ThreadsInsightsService.InsightsDashboard dashboard = insightsService.getInsightsDashboard(userId);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/performance/{userId}")
    @Operation(summary = "Get post performance analytics", description = "Get detailed post performance analytics")
    public ResponseEntity<ThreadsInsightsService.PostPerformanceAnalytics> getPostPerformanceAnalytics(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Days back") @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Access token (optional)") @RequestParam(required = false) String accessToken) {

        java.time.LocalDateTime endDate = java.time.LocalDateTime.now();
        java.time.LocalDateTime startDate = endDate.minusDays(days);

        ThreadsInsightsService.PostPerformanceAnalytics analytics = insightsService.getPostPerformanceAnalytics(userId,
                startDate, endDate);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/post/{postId}/detailed")
    @Operation(summary = "Get detailed post insights", description = "Get comprehensive insights for a specific post including engagement metrics, trends, and comparisons")
    public ResponseEntity<ThreadsInsightsService.PostDetailedInsights> getDetailedPostInsights(
            @Parameter(description = "Post ID") @PathVariable String postId,
            @Parameter(description = "User ID") @RequestParam String userId,
            @Parameter(description = "Access token") @RequestParam String accessToken) {

        ThreadsInsightsService.PostDetailedInsights insights = insightsService.getDetailedPostInsights(postId, userId,
                accessToken);
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/trends/{userId}")
    @Operation(summary = "Get engagement trends", description = "Get engagement trends over time")
    public ResponseEntity<List<ThreadsInsightsService.EngagementTrend>> getEngagementTrends(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Days back") @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Access token (optional)") @RequestParam(required = false) String accessToken) {

        List<ThreadsInsightsService.EngagementTrend> trends = insightsService.getEngagementTrends(userId, days);
        return ResponseEntity.ok(trends);
    }

    @PostMapping("/refresh/{userId}")
    @Operation(summary = "Refresh insights data", description = "Fetch fresh insights from Threads API and update stored data")
    public ResponseEntity<String> refreshInsights(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Access token") @RequestParam String accessToken) {

        try {
            // Refresh user insights
            insightsService.fetchAndStoreUserInsights(userId, accessToken);

            return ResponseEntity.ok("Insights refreshed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to refresh insights: " + e.getMessage());
        }
    }
}