package com.tadeasfort.threadsapi.repository;

import com.tadeasfort.threadsapi.entity.DiscoveredPost;
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
public interface DiscoveredPostRepository extends JpaRepository<DiscoveredPost, Long> {

    // Find discovered posts by user ID
    Page<DiscoveredPost> findByUserIdOrderByDiscoveredAtDesc(String userId, Pageable pageable);

    // Find discovered posts by keyword
    Page<DiscoveredPost> findByKeywordOrderByEngagementScoreDesc(String keyword, Pageable pageable);

    // Find discovered posts by user and keyword
    Page<DiscoveredPost> findByUserIdAndKeywordOrderByEngagementScoreDesc(String userId, String keyword,
            Pageable pageable);

    // Find post by post ID and user
    Optional<DiscoveredPost> findByPostIdAndUserId(String postId, String userId);

    // Find top posts by engagement score
    @Query("SELECT dp FROM DiscoveredPost dp WHERE dp.userId = :userId ORDER BY dp.engagementScore DESC")
    Page<DiscoveredPost> findTopPostsByEngagementScore(@Param("userId") String userId, Pageable pageable);

    // Find unprocessed posts
    List<DiscoveredPost> findByUserIdAndIsProcessedFalseOrderByEngagementScoreDesc(String userId);

    // Find posts above engagement threshold
    @Query("SELECT dp FROM DiscoveredPost dp WHERE dp.userId = :userId AND dp.engagementScore >= :threshold " +
            "ORDER BY dp.engagementScore DESC")
    List<DiscoveredPost> findPostsAboveThreshold(@Param("userId") String userId, @Param("threshold") Double threshold);

    // Find posts ready for interaction (high engagement, not yet interacted)
    @Query("SELECT dp FROM DiscoveredPost dp WHERE dp.userId = :userId AND dp.isInteracted = false AND " +
            "dp.engagementScore >= :threshold ORDER BY dp.engagementScore DESC")
    List<DiscoveredPost> findPostsReadyForInteraction(@Param("userId") String userId,
            @Param("threshold") Double threshold);

    // Find posts discovered in date range
    @Query("SELECT dp FROM DiscoveredPost dp WHERE dp.userId = :userId AND " +
            "dp.discoveredAt BETWEEN :startDate AND :endDate ORDER BY dp.discoveredAt DESC")
    List<DiscoveredPost> findPostsDiscoveredInDateRange(@Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Count posts by keyword
    long countByUserIdAndKeyword(String userId, String keyword);

    // Count posts discovered today
    @Query("SELECT COUNT(dp) FROM DiscoveredPost dp WHERE dp.userId = :userId AND " +
            "DATE(dp.discoveredAt) = DATE(:today)")
    long countPostsDiscoveredToday(@Param("userId") String userId, @Param("today") LocalDateTime today);

    // Get engagement statistics
    @Query("SELECT AVG(dp.engagementScore), MAX(dp.engagementScore), COUNT(dp) FROM DiscoveredPost dp " +
            "WHERE dp.userId = :userId AND dp.keyword = :keyword")
    Object[] getEngagementStatsByKeyword(@Param("userId") String userId, @Param("keyword") String keyword);

    // Find duplicate posts (same post ID discovered multiple times)
    @Query("SELECT dp FROM DiscoveredPost dp WHERE dp.postId = :postId ORDER BY dp.discoveredAt DESC")
    List<DiscoveredPost> findDuplicatesByPostId(@Param("postId") String postId);

    // Check if post already discovered
    boolean existsByPostIdAndUserIdAndKeyword(String postId, String userId, String keyword);

    // Find posts by author username
    List<DiscoveredPost> findByUserIdAndUsernameOrderByEngagementScoreDesc(String userId, String username);

    // Find trending posts (high engagement in last 24 hours)
    @Query("SELECT dp FROM DiscoveredPost dp WHERE dp.discoveredAt >= :since AND " +
            "dp.engagementScore >= :threshold ORDER BY dp.engagementScore DESC")
    List<DiscoveredPost> findTrendingPosts(@Param("since") LocalDateTime since, @Param("threshold") Double threshold);

    // Get keyword performance summary
    @Query("SELECT dp.keyword, COUNT(dp), AVG(dp.engagementScore), MAX(dp.engagementScore) " +
            "FROM DiscoveredPost dp WHERE dp.userId = :userId " +
            "GROUP BY dp.keyword ORDER BY AVG(dp.engagementScore) DESC")
    List<Object[]> getKeywordPerformanceSummary(@Param("userId") String userId);
}