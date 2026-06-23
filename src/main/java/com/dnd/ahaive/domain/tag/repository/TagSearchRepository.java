package com.dnd.ahaive.domain.tag.repository;

import com.dnd.ahaive.domain.tag.entity.TagSearch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagSearchRepository extends JpaRepository<TagSearch, Long> {

    @Query(value = """
            SELECT * FROM tag_search
            WHERE user_uuid = :uuid
            AND tag_name ILIKE %:searchTerm%
            """, nativeQuery = true)
    List<TagSearch> searchTags(@Param("uuid") String uuid,
                               @Param("searchTerm") String searchTerm);

    @Query(value = """
            SELECT * FROM tag_search
            WHERE user_uuid = :uuid
            AND tag_name LIKE %:searchTerm%
            """, nativeQuery = true)
    List<TagSearch> searchTagsUsingBigm(@Param("uuid") String uuid,
                               @Param("searchTerm") String searchTerm);

}
