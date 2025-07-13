package com.tadeasfort.threadsapi.controller;

import com.tadeasfort.threadsapi.entity.ThreadsPost;
import com.tadeasfort.threadsapi.service.ThreadsPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts Management", description = "Threads posts retrieval, management, and analytics")
public class PostsController {

    @Autowired
    private ThreadsPostService postsService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's posts", description = "Retrieve posts for a specific user")
    public ResponseEntity<List<ThreadsPost>> getUserPosts(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Access token") @RequestParam String accessToken) {

        List<ThreadsPost> posts = postsService.retrieveAndStoreUserPosts(userId, accessToken);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}/paginated")
    @Operation(summary = "Get user's posts with pagination", description = "Retrieve paginated posts for a specific user")
    public ResponseEntity<Page<ThreadsPost>> getUserPostsPaginated(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ThreadsPost> posts = postsService.getUserPosts(userId, pageable);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}/insights")
    @Operation(summary = "Get user's posts with insights", description = "Retrieve posts with performance insights")
    public ResponseEntity<List<ThreadsPost>> getUserPostsWithInsights(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Access token") @RequestParam String accessToken) {

        List<ThreadsPost> posts = postsService.getPostsWithInsights(userId, accessToken);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get post by ID", description = "Retrieve a specific post by its ID")
    public ResponseEntity<ThreadsPost> getPostById(
            @Parameter(description = "Post ID") @PathVariable String postId) {

        Optional<ThreadsPost> post = postsService.getPostById(postId);
        return post.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{postId}/insights")
    @Operation(summary = "Update post insights", description = "Refresh insights data for a specific post")
    public ResponseEntity<String> updatePostInsights(
            @Parameter(description = "Post ID") @PathVariable String postId,
            @Parameter(description = "Access token") @RequestParam String accessToken) {

        postsService.updatePostInsights(postId, accessToken);
        return ResponseEntity.ok("Insights updated successfully");
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete post", description = "Delete a post via Threads API")
    public ResponseEntity<String> deletePost(
            @Parameter(description = "Post ID") @PathVariable String postId,
            @Parameter(description = "Access token") @RequestParam String accessToken) {

        boolean deleted = postsService.deletePost(postId, accessToken);
        if (deleted) {
            return ResponseEntity.ok("Post deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to delete post");
        }
    }

    @GetMapping("/top/views")
    @Operation(summary = "Get top posts by views", description = "Retrieve posts with highest view counts")
    public ResponseEntity<Page<ThreadsPost>> getTopPostsByViews(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ThreadsPost> posts = postsService.getTopPostsByViews(pageable);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/top/likes")
    @Operation(summary = "Get top posts by likes", description = "Retrieve posts with highest like counts")
    public ResponseEntity<Page<ThreadsPost>> getTopPostsByLikes(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ThreadsPost> posts = postsService.getTopPostsByLikes(pageable);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}/date-range")
    @Operation(summary = "Get posts in date range", description = "Retrieve user's posts within a specific date range")
    public ResponseEntity<List<ThreadsPost>> getPostsInDateRange(
            @Parameter(description = "User ID") @PathVariable String userId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<ThreadsPost> posts = postsService.getPostsInDateRange(userId, startDate, endDate);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/user/{userId}/statistics")
    @Operation(summary = "Get user statistics", description = "Get comprehensive statistics for a user's posts")
    public ResponseEntity<ThreadsPostService.PostStatistics> getUserStatistics(
            @Parameter(description = "User ID") @PathVariable String userId) {

        ThreadsPostService.PostStatistics stats = postsService.getUserStatistics(userId);
        return ResponseEntity.ok(stats);
    }
}