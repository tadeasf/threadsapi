package com.tadeasfort.threadsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

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

    @JsonProperty("children")
    private List<String> children; // For carousel posts - list of child media IDs

    @JsonProperty("reply_to_id")
    private String replyToId; // For reply posts

    @JsonProperty("quote_post_id")
    private String quotePostId; // For quote posts

    @JsonProperty("location_name")
    private String locationName; // For location tagging

    @JsonProperty("alt_text")
    private String altText; // For accessibility

    @JsonProperty("allow_commenting")
    private Boolean allowCommenting = true; // Whether to allow comments

    @JsonProperty("hide_like_view_counts")
    private Boolean hideLikeViewCounts = false; // Whether to hide like/view counts

    // Additional fields for API usage
    private String userId; // User making the request
    private String accessToken; // Access token for API calls

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

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        this.children = children;
    }

    public String getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(String replyToId) {
        this.replyToId = replyToId;
    }

    public String getQuotePostId() {
        return quotePostId;
    }

    public void setQuotePostId(String quotePostId) {
        this.quotePostId = quotePostId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public Boolean getAllowCommenting() {
        return allowCommenting;
    }

    public void setAllowCommenting(Boolean allowCommenting) {
        this.allowCommenting = allowCommenting;
    }

    public Boolean getHideLikeViewCounts() {
        return hideLikeViewCounts;
    }

    public void setHideLikeViewCounts(Boolean hideLikeViewCounts) {
        this.hideLikeViewCounts = hideLikeViewCounts;
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