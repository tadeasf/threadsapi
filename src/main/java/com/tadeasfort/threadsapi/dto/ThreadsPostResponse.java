package com.tadeasfort.threadsapi.dto;

public class ThreadsPostResponse {

    private String id;

    // Constructors
    public ThreadsPostResponse() {
    }

    public ThreadsPostResponse(String id) {
        this.id = id;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}