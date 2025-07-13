package com.tadeasfort.threadsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "keyword_subscriptions")
public class KeywordSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "keyword", nullable = false)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type")
    private SearchType searchType = SearchType.TOP;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "engagement_threshold")
    private Integer engagementThreshold = 100; // Minimum engagement score to consider

    @Column(name = "max_posts_per_search")
    private Integer maxPostsPerSearch = 50;

    @Column(name = "search_frequency_hours")
    private Integer searchFrequencyHours = 6; // How often to search (in hours)

    @Column(name = "last_search_at")
    private LocalDateTime lastSearchAt;

    @Column(name = "total_searches")
    private Long totalSearches = 0L;

    @Column(name = "total_posts_found")
    private Long totalPostsFound = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public KeywordSubscription() {
    }

    public KeywordSubscription(String userId, String keyword) {
        this.userId = userId;
        this.keyword = keyword;
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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public SearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchType searchType) {
        this.searchType = searchType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getEngagementThreshold() {
        return engagementThreshold;
    }

    public void setEngagementThreshold(Integer engagementThreshold) {
        this.engagementThreshold = engagementThreshold;
    }

    public Integer getMaxPostsPerSearch() {
        return maxPostsPerSearch;
    }

    public void setMaxPostsPerSearch(Integer maxPostsPerSearch) {
        this.maxPostsPerSearch = maxPostsPerSearch;
    }

    public Integer getSearchFrequencyHours() {
        return searchFrequencyHours;
    }

    public void setSearchFrequencyHours(Integer searchFrequencyHours) {
        this.searchFrequencyHours = searchFrequencyHours;
    }

    public LocalDateTime getLastSearchAt() {
        return lastSearchAt;
    }

    public void setLastSearchAt(LocalDateTime lastSearchAt) {
        this.lastSearchAt = lastSearchAt;
    }

    public Long getTotalSearches() {
        return totalSearches;
    }

    public void setTotalSearches(Long totalSearches) {
        this.totalSearches = totalSearches;
    }

    public Long getTotalPostsFound() {
        return totalPostsFound;
    }

    public void setTotalPostsFound(Long totalPostsFound) {
        this.totalPostsFound = totalPostsFound;
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
    public enum SearchType {
        TOP,
        RECENT
    }
}