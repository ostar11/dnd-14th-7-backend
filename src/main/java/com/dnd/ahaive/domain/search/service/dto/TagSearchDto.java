package com.dnd.ahaive.domain.search.service.dto;

import com.dnd.ahaive.domain.tag.entity.TagSearch;

public record TagSearchDto(
        Long tagId,
        String tagName,
        Long insightCount
) {
    public static TagSearchDto ofTagSearch(TagSearch tagSearch) {
        return new TagSearchDto(
                tagSearch.getId(),
                tagSearch.getTagName(),
                tagSearch.getInsightCount()
        );
    }
}
