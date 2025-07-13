package com.tadeasfort.threadsapi.repository;

import com.tadeasfort.threadsapi.entity.SearchResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchResultRepository extends JpaRepository<SearchResult, Long> {

    // Find search results by query
    List<SearchResult> findByQueryOrderBySearchTimestampDesc(String query);

    // Find search results by user
    List<SearchResult> findByUserIdOrderBySearchTimestampDesc(String userId);

    // Find search results by query and user
    List<SearchResult> findByQueryAndUserIdOrderBySearchTimestampDesc(String query, String userId);

    // Find recent search results for a query (within last 24 hours)
    @Query("SELECT sr FROM SearchResult sr WHERE sr.query = :query AND sr.searchTimestamp >= :since ORDER BY sr.searchTimestamp DESC")
    List<SearchResult> findRecentSearchResults(@Param("query") String query, @Param("since") LocalDateTime since);

    // Find search results by search type
    List<SearchResult> findBySearchTypeOrderBySearchTimestampDesc(String searchType);

    // Get popular search queries
    @Query("SELECT sr.query, COUNT(sr) as searchCount FROM SearchResult sr GROUP BY sr.query ORDER BY searchCount DESC")
    List<Object[]> findPopularQueries(Pageable pageable);

    // Get user's search history
    @Query("SELECT DISTINCT sr.query FROM SearchResult sr WHERE sr.userId = :userId ORDER BY sr.searchTimestamp DESC")
    List<String> findUserSearchHistory(@Param("userId") String userId, Pageable pageable);

    // Find search results within date range
    @Query("SELECT sr FROM SearchResult sr WHERE sr.searchTimestamp BETWEEN :startDate AND :endDate ORDER BY sr.searchTimestamp DESC")
    List<SearchResult> findSearchResultsInDateRange(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Count searches for a specific query
    Long countByQuery(String query);

    // Count searches by user
    Long countByUserId(String userId);

    // Delete old search results (for cleanup)
    void deleteBySearchTimestampBefore(LocalDateTime cutoffDate);
}