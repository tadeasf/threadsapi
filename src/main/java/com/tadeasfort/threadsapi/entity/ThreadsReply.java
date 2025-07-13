package com.tadeasfort.threadsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "threads_replies")
public class ThreadsReply {

    @Id
    private String id; // Reply media ID

    @Column(name = "user_id")
    private String userId; // Reply author user ID

    @Column(name = "username")
    private String username;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private ThreadsPost.MediaType mediaType;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "permalink")
    private String permalink;

    @Column(name = "shortcode")
    private String shortcode;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "replied_to_id")
    private String repliedToId;

    @Column(name = "root_post_id")
    private String rootPostId;

    // Relationship to parent post
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_post_id")
    private ThreadsPost parentPost;

    // Insights data
    @Column(name = "likes_count")
    private Long likesCount = 0L;

    @Column(name = "replies_count")
    private Long repliesCount = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    // Constructors
    public ThreadsReply() {
    }

    public ThreadsReply(String id, String userId, String username) {
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

    public ThreadsPost.MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(ThreadsPost.MediaType mediaType) {
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public ThreadsPost getParentPost() {
        return parentPost;
    }

    public void setParentPost(ThreadsPost parentPost) {
        this.parentPost = parentPost;
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
}