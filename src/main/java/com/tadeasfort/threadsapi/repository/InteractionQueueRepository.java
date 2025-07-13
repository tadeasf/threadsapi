package com.tadeasfort.threadsapi.repository;

import com.tadeasfort.threadsapi.entity.InteractionQueue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InteractionQueueRepository extends JpaRepository<InteractionQueue, Long> {

    // Find queue items by user ID
    Page<InteractionQueue> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    // Find queue items by status
    List<InteractionQueue> findByStatusOrderByPriorityDescCreatedAtAsc(InteractionQueue.QueueStatus status);

    // Find queue items by user and status
    List<InteractionQueue> findByUserIdAndStatusOrderByPriorityDescCreatedAtAsc(String userId,
            InteractionQueue.QueueStatus status);

    // Find queue items ready for execution
    @Query("SELECT iq FROM InteractionQueue iq WHERE iq.status = :status AND " +
            "(iq.scheduledFor IS NULL OR iq.scheduledFor <= :now) ORDER BY iq.priority DESC, iq.createdAt ASC")
    List<InteractionQueue> findReadyForExecution(@Param("status") InteractionQueue.QueueStatus status,
            @Param("now") LocalDateTime now);

    // Find queue items by interaction type
    List<InteractionQueue> findByUserIdAndInteractionTypeOrderByCreatedAtDesc(String userId,
            InteractionQueue.InteractionType interactionType);

    // Find queue items by post ID
    Optional<InteractionQueue> findByPostIdAndUserIdAndInteractionType(String postId, String userId,
            InteractionQueue.InteractionType interactionType);

    // Check if interaction already queued
    boolean existsByPostIdAndUserIdAndInteractionType(String postId, String userId,
            InteractionQueue.InteractionType interactionType);

    // Find high priority queue items
    List<InteractionQueue> findByUserIdAndPriorityGreaterThanEqualAndStatusOrderByPriorityDescCreatedAtAsc(
            String userId, Integer priority, InteractionQueue.QueueStatus status);

    // Find queue items scheduled for specific time range
    @Query("SELECT iq FROM InteractionQueue iq WHERE iq.userId = :userId AND " +
            "iq.scheduledFor BETWEEN :startTime AND :endTime ORDER BY iq.scheduledFor ASC")
    List<InteractionQueue> findScheduledInRange(@Param("userId") String userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    // Get queue statistics
    @Query("SELECT iq.status, COUNT(iq) FROM InteractionQueue iq WHERE iq.userId = :userId GROUP BY iq.status")
    List<Object[]> getQueueStatsByUser(@Param("userId") String userId);

    // Count pending items by user
    long countByUserIdAndStatus(String userId, InteractionQueue.QueueStatus status);

    // Find failed queue items for retry
    @Query("SELECT iq FROM InteractionQueue iq WHERE iq.status = :status AND iq.executedAt < :retryAfter " +
            "ORDER BY iq.priority DESC, iq.executedAt ASC")
    List<InteractionQueue> findFailedItemsForRetry(@Param("status") InteractionQueue.QueueStatus status,
            @Param("retryAfter") LocalDateTime retryAfter);

    // Delete old completed/failed items
    void deleteByStatusAndExecutedAtBefore(InteractionQueue.QueueStatus status, LocalDateTime cutoffDate);

    // Find queue items by discovered post ID
    List<InteractionQueue> findByDiscoveredPostIdOrderByCreatedAtDesc(Long discoveredPostId);

    // Get interaction type statistics
    @Query("SELECT iq.interactionType, COUNT(iq), AVG(iq.engagementScore) FROM InteractionQueue iq " +
            "WHERE iq.userId = :userId AND iq.status = :status GROUP BY iq.interactionType")
    List<Object[]> getInteractionTypeStats(@Param("userId") String userId,
            @Param("status") InteractionQueue.QueueStatus status);
}