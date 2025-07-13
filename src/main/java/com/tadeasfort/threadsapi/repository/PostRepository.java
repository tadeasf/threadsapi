package com.tadeasfort.threadsapi.repository;

import com.tadeasfort.threadsapi.entity.Post;
import com.tadeasfort.threadsapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * Find post by creation ID
     */
    Optional<Post> findByCreationId(String creationId);

    /**
     * Find post by Threads post ID
     */
    Optional<Post> findByThreadsPostId(String threadsPostId);

    /**
     * Find all posts by user
     */
    List<Post> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find all published posts by user
     */
    List<Post> findByUserAndIsPublishedTrueOrderByCreatedAtDesc(User user);

    /**
     * Find all unpublished posts by user
     */
    List<Post> findByUserAndIsPublishedFalseOrderByCreatedAtDesc(User user);

    /**
     * Count posts by user
     */
    long countByUser(User user);

    /**
     * Count published posts by user
     */
    long countByUserAndIsPublishedTrue(User user);
}