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
    @Operation(summary = "Get user insights dashboard", description = "Get comprehensive insights dashboard data")
    public ResponseEntity<ThreadsInsightsService.UserInsightsDashboard> getUserInsightsDashboard(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Access token (optional)") @RequestParam(required = false) String accessToken) {

        ThreadsInsightsService.UserInsightsDashboard dashboard = insightsService.getUserInsightsDashboard(userId);
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

    @GetMapping("/trends/{userId}")
    @Operation(summary = "Get engagement trends", description = "Get engagement trends over time")
    public ResponseEntity<List<ThreadsInsightsService.EngagementTrend>> getEngagementTrends(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Days back") @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Access token (optional)") @RequestParam(required = false) String accessToken) {

        List<ThreadsInsightsService.EngagementTrend> trends = insightsService.getEngagementTrends(userId, days);
        return ResponseEntity.ok(trends);
    }
}