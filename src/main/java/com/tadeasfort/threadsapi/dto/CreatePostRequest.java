package com.tadeasfort.threadsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreatePostRequest {

    @JsonProperty("media_type")
    private String mediaType;

    private String text;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("video_url")
    private String videoUrl;

    @JsonProperty("is_carousel_item")
    private Boolean isCarouselItem;

    // Constructors
    public CreatePostRequest() {
    }

    public CreatePostRequest(String mediaType, String text) {
        this.mediaType = mediaType;
        this.text = text;
    }

    // Getters and setters
    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Boolean getIsCarouselItem() {
        return isCarouselItem;
    }

    public void setIsCarouselItem(Boolean isCarouselItem) {
        this.isCarouselItem = isCarouselItem;
    }
}