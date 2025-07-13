package com.tadeasfort.threadsapi.service;

import com.tadeasfort.threadsapi.entity.DiscoveredPost;
import com.tadeasfort.threadsapi.entity.InteractionQueue;
import com.tadeasfort.threadsapi.repository.DiscoveredPostRepository;
import com.tadeasfort.threadsapi.repository.InteractionQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class InteractionQueueService {

    private static final Logger logger = LoggerFactory.getLogger(InteractionQueueService.class);

    // Configuration constants
    private static final double HIGH_ENGAGEMENT_THRESHOLD = 100.0;
    private static final int MAX_QUEUE_SIZE_PER_USER = 1000;
    private static final int DEFAULT_PRIORITY = 1;
    private static final int HIGH_PRIORITY = 3;
    private static final int MAX_PRIORITY = 5;

    @Autowired
    private InteractionQueueRepository queueRepository;

    @Autowired
    private DiscoveredPostRepository discoveredPostRepository;

    /**
     * Add a discovered post to the interaction queue
     */
    public InteractionQueue queueInteraction(String userId, String postId,
            InteractionQueue.InteractionType interactionType,
            Double engagementScore, String reason) {

        // Check if interaction already queued
        if (queueRepository.existsByPostIdAndUserIdAndInteractionType(postId, userId, interactionType)) {
            logger.debug("Interaction already queued for post {} by user {}", postId, userId);
            return null;
        }

        // Check queue size limits
        long pendingCount = queueRepository.countByUserIdAndStatus(userId, InteractionQueue.QueueStatus.PENDING);
        if (pendingCount >= MAX_QUEUE_SIZE_PER_USER) {
            logger.warn("Queue size limit reached for user {}: {} items", userId, pendingCount);
            return null;
        }

        InteractionQueue queueItem = new InteractionQueue(userId, postId, interactionType);
        queueItem.setEngagementScore(engagementScore);
        queueItem.setReason(reason);
        queueItem.setPriority(calculatePriority(engagementScore, interactionType));

        // Set scheduling based on interaction type
        queueItem.setScheduledFor(calculateScheduledTime(interactionType));

        InteractionQueue savedItem = queueRepository.save(queueItem);
        logger.info("Queued {} interaction for post {} by user {} with priority {}",
                interactionType, postId, userId, savedItem.getPriority());

        return savedItem;
    }

    /**
     * Queue interaction for a discovered post
     */
    public InteractionQueue queueDiscoveredPost(DiscoveredPost discoveredPost,
            InteractionQueue.InteractionType interactionType) {

        // Link to discovered post
        InteractionQueue queueItem = queueInteraction(
                discoveredPost.getUserId(),
                discoveredPost.getPostId(),
                interactionType,
                discoveredPost.getEngagementScore(),
                "Auto-queued from keyword: " + discoveredPost.getKeyword());

        if (queueItem != null) {
            queueItem.setDiscoveredPostId(discoveredPost.getId());
            queueItem = queueRepository.save(queueItem);
        }

        return queueItem;
    }

    /**
     * Process queue items ready for execution
     */
    public List<InteractionQueue> processReadyItems(String userId, int limit) {
        List<InteractionQueue> readyItems = queueRepository.findReadyForExecution(
                InteractionQueue.QueueStatus.PENDING, LocalDateTime.now())
                .stream()
                .filter(item -> item.getUserId().equals(userId))
                .limit(limit)
                .collect(Collectors.toList());

        logger.info("Processing {} ready queue items for user {}", readyItems.size(), userId);
        return readyItems;
    }

    /**
     * Mark queue item as processing
     */
    public InteractionQueue markAsProcessing(Long queueId) {
        Optional<InteractionQueue> optItem = queueRepository.findById(queueId);
        if (optItem.isPresent()) {
            InteractionQueue item = optItem.get();
            item.markAsProcessing();
            return queueRepository.save(item);
        }
        return null;
    }

    /**
     * Mark queue item as completed
     */
    public InteractionQueue markAsCompleted(Long queueId, String result) {
        Optional<InteractionQueue> optItem = queueRepository.findById(queueId);
        if (optItem.isPresent()) {
            InteractionQueue item = optItem.get();
            item.markAsCompleted(result);

            // Update discovered post if linked
            if (item.getDiscoveredPostId() != null) {
                updateDiscoveredPostInteraction(item.getDiscoveredPostId(), item.getInteractionType());
            }

            return queueRepository.save(item);
        }
        return null;
    }

    /**
     * Mark queue item as failed
     */
    public InteractionQueue markAsFailed(Long queueId, String error) {
        Optional<InteractionQueue> optItem = queueRepository.findById(queueId);
        if (optItem.isPresent()) {
            InteractionQueue item = optItem.get();
            item.markAsFailed(error);
            return queueRepository.save(item);
        }
        return null;
    }

    /**
     * Get user's queue items
     */
    public Page<InteractionQueue> getUserQueueItems(String userId, Pageable pageable) {
        return queueRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get queue statistics for user
     */
    public Map<String, Object> getQueueStatistics(String userId) {
        List<Object[]> stats = queueRepository.getQueueStatsByUser(userId);

        Map<String, Long> statusCounts = stats.stream()
                .collect(Collectors.toMap(
                        row -> ((InteractionQueue.QueueStatus) row[0]).name(),
                        row -> (Long) row[1]));

        long totalItems = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        long pendingItems = statusCounts.getOrDefault("PENDING", 0L);
        long completedItems = statusCounts.getOrDefault("COMPLETED", 0L);
        long failedItems = statusCounts.getOrDefault("FAILED", 0L);

        return Map.of(
                "totalItems", totalItems,
                "pendingItems", pendingItems,
                "completedItems", completedItems,
                "failedItems", failedItems,
                "statusBreakdown", statusCounts);
    }

    /**
     * Auto-queue high engagement posts
     */
    public void autoQueueHighEngagementPosts(String userId, double minEngagementScore) {
        List<DiscoveredPost> highEngagementPosts = discoveredPostRepository
                .findPostsAboveThreshold(userId, minEngagementScore);

        for (DiscoveredPost post : highEngagementPosts) {
            if (!post.getIsInteracted()) {
                // Determine interaction type based on engagement score
                InteractionQueue.InteractionType interactionType = determineInteractionType(post.getEngagementScore());
                queueDiscoveredPost(post, interactionType);
            }
        }

        logger.info("Auto-queued {} high engagement posts for user {}",
                highEngagementPosts.size(), userId);
    }

    /**
     * Cancel queue item
     */
    public boolean cancelQueueItem(Long queueId, String userId) {
        Optional<InteractionQueue> optItem = queueRepository.findById(queueId);
        if (optItem.isPresent()) {
            InteractionQueue item = optItem.get();
            if (item.getUserId().equals(userId) && item.getStatus() == InteractionQueue.QueueStatus.PENDING) {
                item.setStatus(InteractionQueue.QueueStatus.CANCELLED);
                queueRepository.save(item);
                return true;
            }
        }
        return false;
    }

    /**
     * Cleanup old queue items
     */
    public void cleanupOldQueueItems(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);

        queueRepository.deleteByStatusAndExecutedAtBefore(InteractionQueue.QueueStatus.COMPLETED, cutoffDate);
        queueRepository.deleteByStatusAndExecutedAtBefore(InteractionQueue.QueueStatus.FAILED, cutoffDate);

        logger.info("Cleaned up queue items older than {} days", daysToKeep);
    }

    /**
     * Get items ready for retry
     */
    public List<InteractionQueue> getFailedItemsForRetry(int hoursBeforeRetry) {
        LocalDateTime retryAfter = LocalDateTime.now().minusHours(hoursBeforeRetry);
        return queueRepository.findFailedItemsForRetry(InteractionQueue.QueueStatus.FAILED, retryAfter);
    }

    // Private helper methods

    private int calculatePriority(Double engagementScore, InteractionQueue.InteractionType interactionType) {
        if (engagementScore == null)
            return DEFAULT_PRIORITY;

        int priority = DEFAULT_PRIORITY;

        // Boost priority based on engagement score
        if (engagementScore > 1000)
            priority = MAX_PRIORITY;
        else if (engagementScore > 500)
            priority = HIGH_PRIORITY + 1;
        else if (engagementScore > HIGH_ENGAGEMENT_THRESHOLD)
            priority = HIGH_PRIORITY;

        // Adjust based on interaction type
        switch (interactionType) {
            case LIKE -> priority = Math.max(1, priority - 1); // Lower priority for likes
            case REPLY -> priority = Math.min(MAX_PRIORITY, priority + 1); // Higher priority for replies
            case QUOTE -> priority = Math.min(MAX_PRIORITY, priority + 1); // Higher priority for quotes
            default -> {
                /* Keep calculated priority */ }
        }

        return Math.min(MAX_PRIORITY, Math.max(1, priority));
    }

    private LocalDateTime calculateScheduledTime(InteractionQueue.InteractionType interactionType) {
        LocalDateTime now = LocalDateTime.now();

        return switch (interactionType) {
            case LIKE -> now.plusMinutes(5); // Quick likes
            case REPLY -> now.plusMinutes(15); // Give time to think about replies
            case REPOST -> now.plusMinutes(10); // Medium delay for reposts
            case QUOTE -> now.plusMinutes(20); // Longer delay for quotes
        };
    }

    private InteractionQueue.InteractionType determineInteractionType(Double engagementScore) {
        if (engagementScore > 500) {
            return InteractionQueue.InteractionType.REPLY; // High engagement deserves a reply
        } else if (engagementScore > 200) {
            return InteractionQueue.InteractionType.QUOTE; // Medium engagement gets a quote
        } else {
            return InteractionQueue.InteractionType.LIKE; // Low engagement gets a like
        }
    }

    private void updateDiscoveredPostInteraction(Long discoveredPostId,
            InteractionQueue.InteractionType interactionType) {
        Optional<DiscoveredPost> optPost = discoveredPostRepository.findById(discoveredPostId);
        if (optPost.isPresent()) {
            DiscoveredPost post = optPost.get();
            post.setIsInteracted(true);
            post.setInteractionType(mapToDiscoveredPostInteractionType(interactionType));
            post.setInteractionTimestamp(LocalDateTime.now());
            discoveredPostRepository.save(post);
        }
    }

    private DiscoveredPost.InteractionType mapToDiscoveredPostInteractionType(
            InteractionQueue.InteractionType queueType) {
        return switch (queueType) {
            case LIKE -> DiscoveredPost.InteractionType.LIKED;
            case REPLY -> DiscoveredPost.InteractionType.REPLIED;
            case REPOST -> DiscoveredPost.InteractionType.REPOSTED;
            case QUOTE -> DiscoveredPost.InteractionType.QUOTED;
        };
    }
}