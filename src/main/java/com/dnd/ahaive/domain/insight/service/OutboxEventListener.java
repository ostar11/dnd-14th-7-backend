package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightOutbox;
import com.dnd.ahaive.domain.insight.repository.InsightOutboxRepository;
import com.dnd.ahaive.domain.insight.repository.InsightRepository;
import com.dnd.ahaive.domain.insight.service.dto.InsightDocumentRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OutboxEventListener {

    private final InsightRepository insightRepository;
    private final InsightOutboxRepository insightOutboxRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    @Transactional
    public void saveOutbox(InsightDocumentRequest request) {
        Long insightId = request.insightId();
        Insight insight = insightRepository.findById(insightId)
                .orElseThrow(() -> new EntityNotFoundException("Insight Entity not found. insightId=" + insightId));

        InsightOutbox insightOutbox = InsightOutbox.pendingFrom(insight);
        insightOutboxRepository.save(insightOutbox);
    }
}
