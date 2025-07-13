package com.tadeasfort.threadsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "threads_posts")
public class ThreadsPost {

    @Id
    private String id; // Threads media ID

    @Column(name = "user_id")
    private String userId; // Owner user ID

    @Column(name = "username")
    private String username;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private MediaType mediaType;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "permalink")
    private String permalink;

    @Column(name = "shortcode")
    private String shortcode;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "has_replies")
    private Boolean hasReplies = false;

    @Column(name = "is_quote_post")
    private Boolean isQuotePost = false;

    @Column(name = "is_reply")
    private Boolean isReply = false;

    @Column(name = "replied_to_id")
    private String repliedToId;

    @Column(name = "root_post_id")
    private String rootPostId;

    // Insights data
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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    // Relationships
    @OneToMany(mappedBy = "parentPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ThreadsReply> replies;

    // Constructors
    public ThreadsPost() {
    }

    public ThreadsPost(String id, String userId, String username) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Lifecycle methods
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getPermalink() {
        return permalink;
    }

    public void setPermalink(String permalink) {
        this.permalink = permalink;
    }

    public String getShortcode() {
        return shortcode;
    }

    public void setShortcode(String shortcode) {
        this.shortcode = shortcode;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public String getRepliedToId() {
        return repliedToId;
    }

    public void setRepliedToId(String repliedToId) {
        this.repliedToId = repliedToId;
    }

    public String getRootPostId() {
        return rootPostId;
    }

    public void setRootPostId(String rootPostId) {
        this.rootPostId = rootPostId;
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

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public List<ThreadsReply> getReplies() {
        return replies;
    }

    public void setReplies(List<ThreadsReply> replies) {
        this.replies = replies;
    }

    // Enum for media types
    public enum MediaType {
        TEXT_POST,
        IMAGE,
        VIDEO,
        CAROUSEL_ALBUM
    }
}