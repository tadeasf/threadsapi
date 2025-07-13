package com.tadeasfort.threadsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interaction_queue")
public class InteractionQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "post_id", nullable = false)
    private String postId; // Threads post ID to interact with

    @Column(name = "discovered_post_id")
    private Long discoveredPostId; // Reference to DiscoveredPost entity

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    private InteractionType interactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QueueStatus status = QueueStatus.PENDING;

    @Column(name = "priority")
    private Integer priority = 1; // 1-5, higher number = higher priority

    @Column(name = "engagement_score")
    private Double engagementScore;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // Why this post was queued

    @Column(name = "suggested_content", columnDefinition = "TEXT")
    private String suggestedContent; // AI-generated reply or quote content

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor; // When to execute the interaction

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "execution_result", columnDefinition = "TEXT")
    private String executionResult; // Success message or error details

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public InteractionQueue() {
    }

    public InteractionQueue(String userId, String postId, InteractionType interactionType) {
        this.userId = userId;
        this.postId = postId;
        this.interactionType = interactionType;
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

    // Utility methods
    public void markAsProcessing() {
        this.status = QueueStatus.PROCESSING;
    }

    public void markAsCompleted(String result) {
        this.status = QueueStatus.COMPLETED;
        this.executedAt = LocalDateTime.now();
        this.executionResult = result;
    }

    public void markAsFailed(String error) {
        this.status = QueueStatus.FAILED;
        this.executedAt = LocalDateTime.now();
        this.executionResult = error;
    }

    public void markAsSkipped(String reason) {
        this.status = QueueStatus.SKIPPED;
        this.executedAt = LocalDateTime.now();
        this.executionResult = reason;
    }

    public boolean isReadyForExecution() {
        return status == QueueStatus.PENDING &&
                (scheduledFor == null || scheduledFor.isBefore(LocalDateTime.now()));
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

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public Long getDiscoveredPostId() {
        return discoveredPostId;
    }

    public void setDiscoveredPostId(Long discoveredPostId) {
        this.discoveredPostId = discoveredPostId;
    }

    public InteractionType getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(InteractionType interactionType) {
        this.interactionType = interactionType;
    }

    public QueueStatus getStatus() {
        return status;
    }

    public void setStatus(QueueStatus status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Double getEngagementScore() {
        return engagementScore;
    }

    public void setEngagementScore(Double engagementScore) {
        this.engagementScore = engagementScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSuggestedContent() {
        return suggestedContent;
    }

    public void setSuggestedContent(String suggestedContent) {
        this.suggestedContent = suggestedContent;
    }

    public LocalDateTime getScheduledFor() {
        return scheduledFor;
    }

    public void setScheduledFor(LocalDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String executionResult) {
        this.executionResult = executionResult;
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
        LIKE,
        REPLY,
        REPOST,
        QUOTE
    }

    public enum QueueStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        SKIPPED,
        CANCELLED
    }
}