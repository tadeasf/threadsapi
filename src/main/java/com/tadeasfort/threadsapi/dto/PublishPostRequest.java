package com.tadeasfort.threadsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PublishPostRequest {

    @JsonProperty("creation_id")
    private String creationId;

    private String userId;
    private String accessToken;

    // Constructors
    public PublishPostRequest() {
    }

    public PublishPostRequest(String creationId, String accessToken) {
        this.creationId = creationId;
        this.accessToken = accessToken;
    }

    // Getters and setters
    public String getCreationId() {
        return creationId;
    }

    public void setCreationId(String creationId) {
        this.creationId = creationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}