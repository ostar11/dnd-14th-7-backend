package com.dnd.ahaive.domain.search.service.dto;

import com.dnd.ahaive.domain.insight.document.InsightDocument;
import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightSearch;
import java.time.LocalDateTime;

public record InsightSearchDto(
        String title,
        String initialThought,
        String content,
        LocalDateTime createdDate
) {
    public static InsightSearchDto of(Insight insight, String content) {
        return new InsightSearchDto(
                insight.getTitle(),
                insight.getInitThought(),
                content,
                insight.getCreatedAt()
        );
    }

    public static InsightSearchDto ofDocument(InsightDocument insightDocument) {
        return new InsightSearchDto(
                insightDocument.getTitle(),
                insightDocument.getInitThought(),
                insightDocument.getFirstInsightPiece(),
                insightDocument.getCreatedAt()
        );
    }

    public static InsightSearchDto ofInsightSearch(InsightSearch insightSearch) {
        return new InsightSearchDto(
                insightSearch.getTitle(),
                insightSearch.getInitThought(),
                insightSearch.getFirstInsightPiece(),
                insightSearch.getCreatedAt()
        );
    }
}
