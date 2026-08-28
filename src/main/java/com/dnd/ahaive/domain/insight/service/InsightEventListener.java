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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class InsightEventListener {

    private final InsightOutboxRepository insightOutboxRepository;
    private final InsightRepository insightRepository;
    private final InsightPieceRepository insightPieceRepository;
    private final ElasticsearchInsightService elasticsearchInsightService;

    @Transactional(propagation = Propagation.REQUIRED)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void saveInsightDocument(InsightDocumentRequest request) {
        Long insightId = request.insightId();

        if (insightOutboxRepository.existsByInsightIdAndStatus(insightId, OutboxStatus.COMPLETED)) {
            return;
        }

        Insight insight = insightRepository.findByIdWithUser(insightId)
                .orElseThrow(() -> new EntityNotFoundException("Insight save fail, insightId=" + insightId));

        InsightPiece insightPiece = insightPieceRepository.findByInsightAndCreatedType(insight,
                        InsightGenerationType.INIT)
                .orElseThrow(() ->
                        new EntityNotFoundException("InsightPiece Entity not found. insightId=" + insightId));

        elasticsearchInsightService.saveDocument(InsightDocument.from(insight, insightPiece));

        InsightOutbox insightOutbox = insightOutboxRepository.findByInsight(insight)
                .orElseThrow(
                        () -> new EntityNotFoundException("InsightOutbox Entity not found. insightId=" + insightId));
        insightOutbox.markCompleted();
    }
}
