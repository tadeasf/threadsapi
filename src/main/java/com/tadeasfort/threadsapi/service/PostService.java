package com.tadeasfort.threadsapi.service;

import com.tadeasfort.threadsapi.dto.CreatePostRequest;
import com.tadeasfort.threadsapi.dto.ThreadsPostResponse;
import com.tadeasfort.threadsapi.entity.Post;
import com.tadeasfort.threadsapi.entity.User;
import com.tadeasfort.threadsapi.repository.PostRepository;
import com.tadeasfort.threadsapi.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ThreadsApiClient threadsApiClient;

    public PostService(PostRepository postRepository, UserRepository userRepository,
            ThreadsApiClient threadsApiClient) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.threadsApiClient = threadsApiClient;
    }

    /**
     * Create a post and save to database
     */
    public Post createPost(String threadsUserId, CreatePostRequest postRequest) {
        try {
            // Find the user
            User user = userRepository.findByThreadsUserId(threadsUserId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + threadsUserId));

            // Create post via Threads API
            ThreadsPostResponse apiResponse = threadsApiClient.createPost(user.getAccessToken(), postRequest);

            // Save post to database
            Post post = new Post(user, postRequest.getMediaType(), postRequest.getText());
            post.setCreationId(apiResponse.getId());
            post.setImageUrl(postRequest.getImageUrl());
            post.setVideoUrl(postRequest.getVideoUrl());

            return postRepository.save(post);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create post: " + e.getMessage(), e);
        }
    }

    /**
     * Publish a post and update database
     */
    public Post publishPost(String threadsUserId, String creationId) {
        try {
            // Find the user
            User user = userRepository.findByThreadsUserId(threadsUserId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + threadsUserId));

            // Find the post
            Post post = postRepository.findByCreationId(creationId)
                    .orElseThrow(() -> new RuntimeException("Post not found: " + creationId));

            // Verify post belongs to user
            if (!post.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Post does not belong to user");
            }

            // Publish via Threads API
            Map<String, Object> apiResponse = threadsApiClient.publishPost(user.getAccessToken(), creationId);

            // Update post in database
            post.setIsPublished(true);
            if (apiResponse.containsKey("id")) {
                post.setThreadsPostId((String) apiResponse.get("id"));
            }

            return postRepository.save(post);

        } catch (Exception e) {
            throw new RuntimeException("Failed to publish post: " + e.getMessage(), e);
        }
    }

    /**
     * Get all posts for a user
     */
    @Transactional(readOnly = true)
    public List<Post> getUserPosts(String threadsUserId) {
        User user = userRepository.findByThreadsUserId(threadsUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + threadsUserId));

        return postRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get published posts for a user
     */
    @Transactional(readOnly = true)
    public List<Post> getUserPublishedPosts(String threadsUserId) {
        User user = userRepository.findByThreadsUserId(threadsUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + threadsUserId));

        return postRepository.findByUserAndIsPublishedTrueOrderByCreatedAtDesc(user);
    }

    /**
     * Get post by creation ID
     */
    @Transactional(readOnly = true)
    public Optional<Post> getPostByCreationId(String creationId) {
        return postRepository.findByCreationId(creationId);
    }

    /**
     * Get user post statistics
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getUserPostStats(String threadsUserId) {
        User user = userRepository.findByThreadsUserId(threadsUserId)
                .orElseThrow(() -> new RuntimeException("User not found: " + threadsUserId));

        long totalPosts = postRepository.countByUser(user);
        long publishedPosts = postRepository.countByUserAndIsPublishedTrue(user);

        return Map.of(
                "totalPosts", totalPosts,
                "publishedPosts", publishedPosts,
                "draftPosts", totalPosts - publishedPosts);
    }
}