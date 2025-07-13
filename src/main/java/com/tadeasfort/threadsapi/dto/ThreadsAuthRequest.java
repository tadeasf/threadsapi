package com.tadeasfort.threadsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ThreadsAuthRequest {

    private String code;

    @JsonProperty("redirect_uri")
    private String redirectUri;

    // Constructors
    public ThreadsAuthRequest() {
    }

    public ThreadsAuthRequest(String code, String redirectUri) {
        this.code = code;
        this.redirectUri = redirectUri;
    }

    // Getters and setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}