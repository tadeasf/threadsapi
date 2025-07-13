package com.tadeasfort.threadsapi.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Threads API Wrapper");
        response.put("version", "1.0.0");
        return response;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("app", "Threads API Wrapper");
        response.put("description", "Spring Boot wrapper for Meta Threads API");
        response.put("version", "1.0.0");
        response.put("endpoints", new String[] {
                "/api/auth/login-url",
                "/api/auth/exchange-token",
                "/api/auth/callback",
                "/api/posts/create",
                "/api/posts/publish",
                "/api/user/profile",
                "/api/user/threads"
        });
        return response;
    }
}