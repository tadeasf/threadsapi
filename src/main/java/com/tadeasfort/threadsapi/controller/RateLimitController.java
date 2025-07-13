package com.tadeasfort.threadsapi.controller;

import com.tadeasfort.threadsapi.service.ThreadsRateLimitService;
import com.tadeasfort.threadsapi.service.ThreadsRateLimitService.UserRateLimitInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/rate-limit")
@Tag(name = "Rate Limiting", description = "Rate limiting information and management")
public class RateLimitController {

    @Autowired
    private ThreadsRateLimitService rateLimitService;

    @GetMapping("/status/{userId}")
    @Operation(summary = "Get rate limit status", description = "Get current rate limiting status for a user")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(
            @Parameter(description = "User ID") @PathVariable String userId) {

        UserRateLimitInfo userInfo = rateLimitService.getUserRateLimitStatus(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("windowStart", userInfo.getWindowStart());
        response.put("impressions", userInfo.getImpressions());

        // API calls
        response.put("callsUsed", userInfo.getCallsInWindow());
        response.put("maxCalls", userInfo.getMaxCallsPerWindow());
        response.put("remainingCalls", userInfo.getMaxCallsPerWindow() - userInfo.getCallsInWindow());

        // Posts
        response.put("postsUsed", userInfo.getPostsInWindow());
        response.put("maxPosts", 250);
        response.put("remainingPosts", 250 - userInfo.getPostsInWindow());

        // Replies
        response.put("repliesUsed", userInfo.getRepliesInWindow());
        response.put("maxReplies", 1000);
        response.put("remainingReplies", 1000 - userInfo.getRepliesInWindow());

        // Usage percentages
        response.put("callsUsagePercent",
                Math.round((double) userInfo.getCallsInWindow() / userInfo.getMaxCallsPerWindow() * 100));
        response.put("postsUsagePercent", Math.round((double) userInfo.getPostsInWindow() / 250 * 100));
        response.put("repliesUsagePercent", Math.round((double) userInfo.getRepliesInWindow() / 1000 * 100));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/impressions/{userId}")
    @Operation(summary = "Update user impressions", description = "Update the impression count for a user (affects rate limits)")
    public ResponseEntity<Map<String, String>> updateImpressions(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "New impression count") @RequestParam int impressions) {

        rateLimitService.updateUserImpressions(userId, impressions);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Impressions updated successfully");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Cleanup old entries", description = "Clean up old rate limiting entries")
    public ResponseEntity<Map<String, String>> cleanupOldEntries() {
        rateLimitService.cleanupOldEntries();

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Old entries cleaned up successfully");

        return ResponseEntity.ok(response);
    }
}