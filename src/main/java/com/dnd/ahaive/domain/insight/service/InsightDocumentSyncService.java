package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.document.InsightDocument;
import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightGenerationType;
import com.dnd.ahaive.domain.insight.entity.InsightOutbox;
import com.dnd.ahaive.domain.insight.entity.InsightPiece;
import com.dnd.ahaive.domain.insight.entity.OutboxStatus;
import com.dnd.ahaive.domain.insight.repository.InsightOutboxRepository;
import com.dnd.ahaive.domain.insight.repository.InsightPieceRepository;
import com.dnd.ahaive.domain.insight.repository.InsightRepository;
import com.dnd.ahaive.domain.insight.service.dto.InsightDocumentRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsightDocumentSyncService {

    private final InsightOutboxRepository insightOutboxRepository;

    private final InsightDocumentReader insightDocumentReader;
    private final ElasticsearchInsightService elasticsearchInsightService;

    private final InsightOutboxService insightOutboxService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncDocument(InsightDocumentRequest request) {
        Long insightId = request.insightId();

        if (insightOutboxRepository.existsByInsightIdAndStatus(insightId, OutboxStatus.COMPLETED)) {
            return;
        }

        InsightDocumentSyncDto insightDocumentSyncDto = insightDocumentReader.loadInsightDocumentRequest(insightId);
        Insight insight = insightDocumentSyncDto.insight();
        InsightPiece insightPiece = insightDocumentSyncDto.insightPiece();

        elasticsearchInsightService.saveDocument(InsightDocument.from(insight, insightPiece));

        insightOutboxService.markOutboxComplete(insight, insightId);
    }

}
