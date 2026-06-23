package com.dnd.ahaive.domain.insight.repository;

import com.dnd.ahaive.domain.insight.entity.InsightSearch;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InsightSearchRepository extends JpaRepository<InsightSearch, Long> {

    @Query(value = """
            SELECT * FROM insight_search
            WHERE user_uuid = :uuid
            AND trash = false
            AND search_text ILIKE %:searchTerm%
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<InsightSearch> searchInsights(@Param("uuid") String uuid,
                                       @Param("searchTerm") String searchTerm,
                                       Pageable pageable);

    @Query(value = """
            SELECT * FROM insight_search
            WHERE user_uuid = :uuid
            AND trash = false
            AND search_text LIKE %:searchTerm%
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<InsightSearch> searchInsightsUsingBigm(@Param("uuid") String uuid,
                                       @Param("searchTerm") String searchTerm,
                                       Pageable pageable);

}
