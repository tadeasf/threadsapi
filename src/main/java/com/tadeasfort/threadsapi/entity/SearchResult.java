package com.tadeasfort.threadsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_results")
public class SearchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query", nullable = false)
    private String query;

    @Column(name = "search_type")
    private String searchType; // TOP or RECENT

    @Column(name = "post_id", nullable = false)
    private String postId;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private ThreadsPost.MediaType mediaType;

    @Column(name = "permalink")
    private String permalink;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "username")
    private String username;

    @Column(name = "has_replies")
    private Boolean hasReplies = false;

    @Column(name = "is_quote_post")
    private Boolean isQuotePost = false;

    @Column(name = "is_reply")
    private Boolean isReply = false;

    @Column(name = "search_timestamp")
    private LocalDateTime searchTimestamp;

    @Column(name = "user_id")
    private String userId; // User who performed the search

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public SearchResult() {
    }

    public SearchResult(String query, String searchType, String userId) {
        this.query = query;
        this.searchType = searchType;
        this.userId = userId;
        this.searchTimestamp = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    // Lifecycle methods
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (searchTimestamp == null) {
            searchTimestamp = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public LocalDateTime getSearchTimestamp() {
        return searchTimestamp;
    }

    public void setSearchTimestamp(LocalDateTime searchTimestamp) {
        this.searchTimestamp = searchTimestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}