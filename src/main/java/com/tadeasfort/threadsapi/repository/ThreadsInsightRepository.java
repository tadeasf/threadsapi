package com.tadeasfort.threadsapi.repository;

import com.tadeasfort.threadsapi.entity.ThreadsInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ThreadsInsightRepository extends JpaRepository<ThreadsInsight, Long> {

    // Find insights by user ID
    List<ThreadsInsight> findByUserIdOrderByDateRecordedDesc(String userId);

    // Find insights by user and insight type
    List<ThreadsInsight> findByUserIdAndInsightTypeOrderByDateRecordedDesc(String userId,
            ThreadsInsight.InsightType insightType);

    // Find insights for a specific post
    List<ThreadsInsight> findByPostIdOrderByDateRecordedDesc(String postId);

    // Find insights by metric name
    List<ThreadsInsight> findByUserIdAndMetricNameOrderByDateRecordedDesc(String userId, String metricName);

    // Find insights within date range
    @Query("SELECT i FROM ThreadsInsight i WHERE i.userId = :userId AND i.dateRecorded BETWEEN :startDate AND :endDate ORDER BY i.dateRecorded DESC")
    List<ThreadsInsight> findInsightsInDateRange(@Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Get latest insight for a specific metric
    @Query("SELECT i FROM ThreadsInsight i WHERE i.userId = :userId AND i.metricName = :metricName ORDER BY i.dateRecorded DESC LIMIT 1")
    ThreadsInsight findLatestInsightByMetric(@Param("userId") String userId, @Param("metricName") String metricName);

    // Get insights for a specific post and metric
    @Query("SELECT i FROM ThreadsInsight i WHERE i.postId = :postId AND i.metricName = :metricName ORDER BY i.dateRecorded DESC")
    List<ThreadsInsight> findPostInsightsByMetric(@Param("postId") String postId,
            @Param("metricName") String metricName);

    // Get user insights summary (latest values for each metric)
    @Query("SELECT i FROM ThreadsInsight i WHERE i.userId = :userId AND i.insightType = 'USER_INSIGHT' AND i.dateRecorded = (SELECT MAX(i2.dateRecorded) FROM ThreadsInsight i2 WHERE i2.userId = i.userId AND i2.metricName = i.metricName)")
    List<ThreadsInsight> findLatestUserInsights(@Param("userId") String userId);

    // Get total metrics for user
    @Query("SELECT SUM(i.metricValue) FROM ThreadsInsight i WHERE i.userId = :userId AND i.metricName = :metricName")
    Long getTotalMetricValue(@Param("userId") String userId, @Param("metricName") String metricName);

    // Get average metric value over time
    @Query("SELECT AVG(i.metricValue) FROM ThreadsInsight i WHERE i.userId = :userId AND i.metricName = :metricName AND i.dateRecorded BETWEEN :startDate AND :endDate")
    Double getAverageMetricValue(@Param("userId") String userId,
            @Param("metricName") String metricName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Delete old insights (for cleanup)
    void deleteByDateRecordedBefore(LocalDateTime cutoffDate);

    // Check if insight exists for specific conditions
    boolean existsByUserIdAndPostIdAndMetricNameAndDateRecorded(String userId, String postId, String metricName,
            LocalDateTime dateRecorded);
}