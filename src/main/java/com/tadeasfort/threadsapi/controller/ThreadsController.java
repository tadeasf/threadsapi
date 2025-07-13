package com.tadeasfort.threadsapi.controller;

import com.tadeasfort.threadsapi.dto.CreatePostRequest;
import com.tadeasfort.threadsapi.dto.ThreadsPostResponse;
import com.tadeasfort.threadsapi.service.ThreadsApiClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ThreadsController {

    private final ThreadsApiClient threadsApiClient;

    public ThreadsController(ThreadsApiClient threadsApiClient) {
        this.threadsApiClient = threadsApiClient;
    }

    @PostMapping("/posts/create")
    public ThreadsPostResponse createPost(@RequestParam String accessToken,
            @RequestBody CreatePostRequest postRequest) {
        try {
            return threadsApiClient.createPost(accessToken, postRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create post: " + e.getMessage());
        }
    }

    @PostMapping("/posts/publish")
    public Map<String, Object> publishPost(@RequestParam String accessToken,
            @RequestParam String creationId) {
        try {
            return threadsApiClient.publishPost(accessToken, creationId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish post: " + e.getMessage());
        }
    }

    @GetMapping("/user/profile")
    public Map<String, Object> getUserProfile(@RequestParam String accessToken) {
        try {
            return threadsApiClient.getUserProfile(accessToken);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get user profile: " + e.getMessage());
        }
    }

    @GetMapping("/user/threads")
    public Map<String, Object> getUserThreads(@RequestParam String accessToken,
            @RequestParam(required = false) String fields,
            @RequestParam(required = false) Integer limit) {
        try {
            return threadsApiClient.getUserThreads(accessToken, fields, limit);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get user threads: " + e.getMessage());
        }
    }
}