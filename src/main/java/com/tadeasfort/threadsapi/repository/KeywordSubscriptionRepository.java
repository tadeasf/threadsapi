package com.tadeasfort.threadsapi.repository;

import com.tadeasfort.threadsapi.entity.KeywordSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordSubscriptionRepository extends JpaRepository<KeywordSubscription, Long> {

    // Find subscriptions by user ID
    List<KeywordSubscription> findByUserIdOrderByCreatedAtDesc(String userId);

    // Find active subscriptions by user ID
    List<KeywordSubscription> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(String userId);

    // Find subscription by user and keyword
    Optional<KeywordSubscription> findByUserIdAndKeyword(String userId, String keyword);

    // Find subscriptions ready for search (based on frequency)
    @Query("SELECT ks FROM KeywordSubscription ks WHERE ks.isActive = true AND " +
            "(ks.lastSearchAt IS NULL OR ks.lastSearchAt <= :cutoffTime)")
    List<KeywordSubscription> findSubscriptionsReadyForSearch(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Find subscriptions by keyword (across all users)
    List<KeywordSubscription> findByKeywordAndIsActiveTrueOrderByCreatedAtDesc(String keyword);

    // Count active subscriptions by user
    long countByUserIdAndIsActiveTrue(String userId);

    // Find top keywords by subscription count
    @Query("SELECT ks.keyword, COUNT(ks) as count FROM KeywordSubscription ks " +
            "WHERE ks.isActive = true GROUP BY ks.keyword ORDER BY count DESC")
    List<Object[]> findTopKeywordsBySubscriptionCount();

    // Find subscriptions with high engagement threshold
    List<KeywordSubscription> findByEngagementThresholdGreaterThanAndIsActiveTrueOrderByEngagementThresholdDesc(
            Integer threshold);

    // Check if user has subscription for keyword
    boolean existsByUserIdAndKeywordAndIsActiveTrue(String userId, String keyword);

    // Find subscriptions that haven't been searched recently
    @Query("SELECT ks FROM KeywordSubscription ks WHERE ks.isActive = true AND " +
            "ks.lastSearchAt < :since ORDER BY ks.lastSearchAt ASC")
    List<KeywordSubscription> findSubscriptionsNotSearchedSince(@Param("since") LocalDateTime since);

    // Get user's keyword statistics
    @Query("SELECT SUM(ks.totalSearches), SUM(ks.totalPostsFound) FROM KeywordSubscription ks " +
            "WHERE ks.userId = :userId AND ks.isActive = true")
    Object[] getUserKeywordStats(@Param("userId") String userId);
}