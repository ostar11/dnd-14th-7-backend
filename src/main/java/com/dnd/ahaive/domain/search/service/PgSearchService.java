package com.dnd.ahaive.domain.search.service;

import com.dnd.ahaive.domain.insight.repository.InsightSearchRepository;
import com.dnd.ahaive.domain.search.controller.dto.SearchResultDto;
import com.dnd.ahaive.domain.search.service.dto.InsightSearchDto;
import com.dnd.ahaive.domain.search.service.dto.TagSearchDto;
import com.dnd.ahaive.domain.tag.repository.TagSearchRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PgSearchService implements SearchService {

    private final InsightSearchRepository insightSearchRepository;
    private final TagSearchRepository tagSearchRepository;

    @Transactional(readOnly = true)
    @Override
    public SearchResultDto search(String uuid, String searchTerm) {
        List<TagSearchDto> tags = tagSearchRepository.searchTags(uuid, searchTerm)
                .stream()
                .map(TagSearchDto::ofTagSearch)
                .toList();

        List<InsightSearchDto> insights = insightSearchRepository
                .searchInsights(uuid, searchTerm, PageRequest.of(0, 20))
                .stream()
                .map(InsightSearchDto::ofInsightSearch)
                .toList();

        return new SearchResultDto(tags, insights);
    }

    @Transactional(readOnly = true)
    public SearchResultDto searchBigm(String uuid, String searchTerm) {
        List<TagSearchDto> tags = tagSearchRepository.searchTagsUsingBigm(uuid, searchTerm)
                .stream()
                .map(TagSearchDto::ofTagSearch)
                .toList();

        List<InsightSearchDto> insights = insightSearchRepository
                .searchInsightsUsingBigm(uuid, searchTerm, PageRequest.of(0, 20))
                .stream()
                .map(InsightSearchDto::ofInsightSearch)
                .toList();

        return new SearchResultDto(tags, insights);
    }

}
