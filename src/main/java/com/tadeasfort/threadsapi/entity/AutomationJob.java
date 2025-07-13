package com.tadeasfort.threadsapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "automation_jobs")
public class AutomationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "job_parameters", columnDefinition = "TEXT")
    private String jobParameters; // JSON string with job-specific parameters

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "run_count")
    private Long runCount = 0L;

    @Column(name = "success_count")
    private Long successCount = 0L;

    @Column(name = "failure_count")
    private Long failureCount = 0L;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public AutomationJob() {
    }

    public AutomationJob(String userId, JobType jobType, String jobParameters) {
        this.userId = userId;
        this.jobType = jobType;
        this.jobParameters = jobParameters;
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
    public void markAsRunning() {
        this.status = JobStatus.RUNNING;
        this.lastRunAt = LocalDateTime.now();
        this.runCount++;
    }

    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.successCount++;
    }

    public void markAsFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.failureCount++;
        this.lastErrorMessage = errorMessage;
    }

    public void scheduleNextRun(LocalDateTime nextRun) {
        this.nextRunAt = nextRun;
        this.status = JobStatus.PENDING;
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

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getJobParameters() {
        return jobParameters;
    }

    public void setJobParameters(String jobParameters) {
        this.jobParameters = jobParameters;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(LocalDateTime nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(LocalDateTime lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public Long getRunCount() {
        return runCount;
    }

    public void setRunCount(Long runCount) {
        this.runCount = runCount;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }

    public Long getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Long failureCount) {
        this.failureCount = failureCount;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
    public enum JobType {
        KEYWORD_SEARCH,
        ENGAGEMENT_ANALYSIS,
        AUTO_REPLY,
        AUTO_REPOST,
        CONTENT_CURATION,
        TREND_ANALYSIS
    }

    public enum JobStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}