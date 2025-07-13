package com.tadeasfort.threadsapi.repository;

import com.tadeasfort.threadsapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by their Threads user ID
     */
    Optional<User> findByThreadsUserId(String threadsUserId);

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if user exists by Threads user ID
     */
    boolean existsByThreadsUserId(String threadsUserId);

    /**
     * Check if user exists by username
     */
    boolean existsByUsername(String username);
}