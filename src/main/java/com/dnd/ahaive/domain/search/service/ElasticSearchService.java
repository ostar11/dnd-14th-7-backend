package com.dnd.ahaive.domain.search.service;

import com.dnd.ahaive.domain.insight.document.InsightDocument;
import com.dnd.ahaive.domain.search.controller.dto.SearchResultDto;
import com.dnd.ahaive.domain.search.service.dto.InsightSearchDto;
import com.dnd.ahaive.domain.search.service.dto.TagSearchDto;
import com.dnd.ahaive.domain.tag.document.TagDocument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

@Primary
@Service
@RequiredArgsConstructor
public class ElasticSearchService implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public SearchResultDto search(String uuid, String searchTerm) {
        List<TagSearchDto> tagSearchDtos = searchTags(uuid, searchTerm);
        List<InsightSearchDto> insightSearchDtos = searchInsights(uuid, searchTerm);
        return new SearchResultDto(tagSearchDtos, insightSearchDtos);
    }

    private List<InsightSearchDto> searchInsights(String uuid, String searchTerm) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b ->
                                b.filter(m -> m.term(t -> t.field("userUuid").value(uuid)))
                                        .filter(m -> m.term(t -> t.field("trash").value(false)))
                                        .must(m -> m.multiMatch(
                                                mm -> mm.fields("title", "initThought", "firstInsightPiece").query(searchTerm)
                                        ))
                        )
                ).withPageable(PageRequest.of(0, 20))
                .build();

        return elasticsearchOperations.search(query, InsightDocument.class)
                .stream()
                .map(hit -> InsightSearchDto.ofDocument(hit.getContent()))
                .toList();
    }

    private List<TagSearchDto> searchTags(String uuid, String searchTerm) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .filter(m -> m.term(t -> t.field("userUuid").value(uuid)))
                                .must(m -> m.wildcard(mm -> mm.field("tagName.keyword").value("*" + searchTerm + "*")))
                        )
                )
                .build();

        return elasticsearchOperations.search(query, TagDocument.class)
                .stream()
                .map(hit -> new TagSearchDto(
                        hit.getContent().getId(),
                        hit.getContent().getTagName(),
                        hit.getContent().getInsightCount()
                ))
                .toList();
    }

}
