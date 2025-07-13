package com.tadeasfort.threadsapi.repository;

import com.tadeasfort.threadsapi.entity.ThreadsPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ThreadsPostRepository extends JpaRepository<ThreadsPost, String> {

        // Find posts by user ID
        List<ThreadsPost> findByUserIdAndIsDeletedFalseOrderByTimestampDesc(String userId);

        // Find posts by user ID with pagination
        Page<ThreadsPost> findByUserIdAndIsDeletedFalse(String userId, Pageable pageable);

        // Find posts by username
        List<ThreadsPost> findByUsernameAndIsDeletedFalseOrderByTimestampDesc(String username);

        // Find posts containing specific text
        @Query("SELECT p FROM ThreadsPost p WHERE p.text LIKE %:text% AND p.isDeleted = false ORDER BY p.timestamp DESC")
        List<ThreadsPost> findByTextContaining(@Param("text") String text);

        // Find posts by media type
        List<ThreadsPost> findByMediaTypeAndIsDeletedFalseOrderByTimestampDesc(ThreadsPost.MediaType mediaType);

        // Find posts within date range
        @Query("SELECT p FROM ThreadsPost p WHERE p.timestamp BETWEEN :startDate AND :endDate AND p.isDeleted = false ORDER BY p.timestamp DESC")
        List<ThreadsPost> findPostsInDateRange(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Find posts by user within date range
        @Query("SELECT p FROM ThreadsPost p WHERE p.userId = :userId AND p.timestamp BETWEEN :startDate AND :endDate AND p.isDeleted = false ORDER BY p.timestamp DESC")
        List<ThreadsPost> findPostsByUserInDateRange(@Param("userId") String userId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Find top posts by views
        @Query("SELECT p FROM ThreadsPost p WHERE p.isDeleted = false ORDER BY p.viewsCount DESC")
        Page<ThreadsPost> findTopPostsByViews(Pageable pageable);

        // Find top posts by likes
        @Query("SELECT p FROM ThreadsPost p WHERE p.isDeleted = false ORDER BY p.likesCount DESC")
        Page<ThreadsPost> findTopPostsByLikes(Pageable pageable);

        // Find posts with replies
        List<ThreadsPost> findByHasRepliesTrueAndIsDeletedFalseOrderByTimestampDesc();

        // Find quote posts
        List<ThreadsPost> findByIsQuotePostTrueAndIsDeletedFalseOrderByTimestampDesc();

        // Find replies to a specific post
        List<ThreadsPost> findByRepliedToIdAndIsDeletedFalseOrderByTimestampDesc(String repliedToId);

        // Get user's post statistics
        @Query("SELECT COUNT(p) FROM ThreadsPost p WHERE p.userId = :userId AND p.isDeleted = false")
        Long countPostsByUser(@Param("userId") String userId);

        @Query("SELECT SUM(p.viewsCount) FROM ThreadsPost p WHERE p.userId = :userId AND p.isDeleted = false")
        Long getTotalViewsByUser(@Param("userId") String userId);

        @Query("SELECT SUM(p.likesCount) FROM ThreadsPost p WHERE p.userId = :userId AND p.isDeleted = false")
        Long getTotalLikesByUser(@Param("userId") String userId);

        // Check if post exists and is not deleted
        boolean existsByIdAndIsDeletedFalse(String id);

        // Find recent posts (last 24 hours)
        @Query("SELECT p FROM ThreadsPost p WHERE p.timestamp >= :since AND p.isDeleted = false ORDER BY p.timestamp DESC")
        List<ThreadsPost> findRecentPosts(@Param("since") LocalDateTime since);
}