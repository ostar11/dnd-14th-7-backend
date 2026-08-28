package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.document.InsightDocument;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ElasticsearchInsightService {

    private final ElasticsearchOperations elasticsearchOperations;

    public Long saveDocument(InsightDocument insightDocument) {
        InsightDocument savedInsightDocument = elasticsearchOperations.save(insightDocument);
        return savedInsightDocument.getId();
    }

    public void saveAll(List<InsightDocument> insightOutboxes) {
        elasticsearchOperations.save(insightOutboxes);
    }
}
