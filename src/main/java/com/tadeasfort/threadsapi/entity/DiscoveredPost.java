package com.tadeasfort.threadsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "discovered_posts")
public class DiscoveredPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private String postId; // Threads post ID

    @Column(name = "keyword", nullable = false)
    private String keyword; // The keyword that found this post

    @Column(name = "user_id", nullable = false)
    private String userId; // User who has the keyword subscription

    @Column(name = "post_user_id")
    private String postUserId; // Author of the discovered post

    @Column(name = "username")
    private String username; // Author username

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private ThreadsPost.MediaType mediaType;

    @Column(name = "permalink")
    private String permalink;

    @Column(name = "post_timestamp")
    private LocalDateTime postTimestamp; // When the original post was created

    @Column(name = "has_replies")
    private Boolean hasReplies = false;

    @Column(name = "is_quote_post")
    private Boolean isQuotePost = false;

    @Column(name = "is_reply")
    private Boolean isReply = false;

    // Engagement metrics (captured at discovery time)
    @Column(name = "views_count")
    private Long viewsCount = 0L;

    @Column(name = "likes_count")
    private Long likesCount = 0L;

    @Column(name = "replies_count")
    private Long repliesCount = 0L;

    @Column(name = "reposts_count")
    private Long repostsCount = 0L;

    @Column(name = "quotes_count")
    private Long quotesCount = 0L;

    // Calculated engagement score
    @Column(name = "engagement_score")
    private Double engagementScore = 0.0;

    // Automation tracking
    @Column(name = "is_processed", nullable = false)
    private Boolean isProcessed = false;

    @Column(name = "is_interacted", nullable = false)
    private Boolean isInteracted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type")
    private InteractionType interactionType;

    @Column(name = "interaction_timestamp")
    private LocalDateTime interactionTimestamp;

    @Column(name = "discovered_at", nullable = false)
    private LocalDateTime discoveredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public DiscoveredPost() {
    }

    public DiscoveredPost(String postId, String keyword, String userId) {
        this.postId = postId;
        this.keyword = keyword;
        this.userId = userId;
        this.discoveredAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Lifecycle methods
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (discoveredAt == null) {
            discoveredAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Utility method to calculate engagement score
    public void calculateEngagementScore() {
        // Weighted engagement score: likes*1 + replies*3 + reposts*2 + quotes*2.5
        double score = (likesCount != null ? likesCount : 0) * 1.0 +
                (repliesCount != null ? repliesCount : 0) * 3.0 +
                (repostsCount != null ? repostsCount : 0) * 2.0 +
                (quotesCount != null ? quotesCount : 0) * 2.5;

        // Add time decay factor (newer posts get higher scores)
        if (postTimestamp != null) {
            long hoursOld = java.time.Duration.between(postTimestamp, LocalDateTime.now()).toHours();
            double timeDecay = Math.max(0.1, 1.0 - (hoursOld / 168.0)); // Decay over 1 week
            score *= timeDecay;
        }

        this.engagementScore = score;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPostUserId() {
        return postUserId;
    }

    public void setPostUserId(String postUserId) {
        this.postUserId = postUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ThreadsPost.MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(ThreadsPost.MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public LocalDateTime getPostTimestamp() {
        return postTimestamp;
    }

    public void setPostTimestamp(LocalDateTime postTimestamp) {
        this.postTimestamp = postTimestamp;
    }

    public Boolean getHasReplies() {
        return hasReplies;
    }

    public void setHasReplies(Boolean hasReplies) {
        this.hasReplies = hasReplies;
    }

    public Boolean getIsQuotePost() {
        return isQuotePost;
    }

    public void setIsQuotePost(Boolean isQuotePost) {
        this.isQuotePost = isQuotePost;
    }

    public Boolean getIsReply() {
        return isReply;
    }

    public void setIsReply(Boolean isReply) {
        this.isReply = isReply;
    }

    public Long getViewsCount() {
        return viewsCount;
    }

    public void setViewsCount(Long viewsCount) {
        this.viewsCount = viewsCount;
    }

    public Long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }

    public Long getRepliesCount() {
        return repliesCount;
    }

    public void setRepliesCount(Long repliesCount) {
        this.repliesCount = repliesCount;
    }

    public Long getRepostsCount() {
        return repostsCount;
    }

    public void setRepostsCount(Long repostsCount) {
        this.repostsCount = repostsCount;
    }

    public Long getQuotesCount() {
        return quotesCount;
    }

    public void setQuotesCount(Long quotesCount) {
        this.quotesCount = quotesCount;
    }

    public Double getEngagementScore() {
        return engagementScore;
    }

    public void setEngagementScore(Double engagementScore) {
        this.engagementScore = engagementScore;
    }

    public Boolean getIsProcessed() {
        return isProcessed;
    }

    public void setIsProcessed(Boolean isProcessed) {
        this.isProcessed = isProcessed;
    }

    public Boolean getIsInteracted() {
        return isInteracted;
    }

    public void setIsInteracted(Boolean isInteracted) {
        this.isInteracted = isInteracted;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(InteractionType interactionType) {
        this.interactionType = interactionType;
    }

    public LocalDateTime getInteractionTimestamp() {
        return interactionTimestamp;
    }

    public void setInteractionTimestamp(LocalDateTime interactionTimestamp) {
        this.interactionTimestamp = interactionTimestamp;
    }

    public LocalDateTime getDiscoveredAt() {
        return discoveredAt;
    }

    public void setDiscoveredAt(LocalDateTime discoveredAt) {
        this.discoveredAt = discoveredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Enums
    public enum InteractionType {
        LIKED,
        REPLIED,
        REPOSTED,
        QUOTED
    }
}