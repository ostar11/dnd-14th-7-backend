package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightOutbox;
import com.dnd.ahaive.domain.insight.repository.InsightOutboxRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsightOutboxService {

    private final InsightOutboxRepository insightOutboxRepository;

    @Transactional
    public void markOutboxComplete(Insight insight, Long insightId) {
        InsightOutbox insightOutbox = insightOutboxRepository.findByInsight(insight)
                .orElseThrow(
                        () -> new EntityNotFoundException("InsightOutbox Entity not found. insightId=" + insightId));
        insightOutbox.markCompleted();
    }
}
