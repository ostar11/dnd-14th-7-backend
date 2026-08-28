package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.document.InsightDocument;
import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightGenerationType;
import com.dnd.ahaive.domain.insight.entity.InsightOutbox;
import com.dnd.ahaive.domain.insight.entity.InsightPiece;
import com.dnd.ahaive.domain.insight.entity.OutboxStatus;
import com.dnd.ahaive.domain.insight.repository.InsightOutboxRepository;
import com.dnd.ahaive.domain.insight.repository.InsightPieceRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsightOutboxRelayScheduler {

    private final InsightOutboxRepository insightOutboxRepository;
    private final InsightPieceRepository insightPieceRepository;
    private final ElasticsearchInsightService elasticsearchInsightService;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void relay() {
        List<InsightOutbox> pendingInsightOutboxes = insightOutboxRepository.findByStatusAndCreatedAtBefore(
                OutboxStatus.PENDING, LocalDateTime.now().minusSeconds(5), PageRequest.of(0, 100));

        if (pendingInsightOutboxes == null || pendingInsightOutboxes.isEmpty()) {
            return;
        }

        List<InsightDocument> documents = new ArrayList<>();
        List<InsightOutbox> resolvedOutboxes = new ArrayList<>();

        for (InsightOutbox insightOutbox : pendingInsightOutboxes) {
            Insight insight = insightOutbox.getInsight();
            Optional<InsightPiece> insightPiece = insightPieceRepository.findByInsightAndCreatedType(insight,
                    InsightGenerationType.INIT);

            if (insightPiece.isEmpty()) {
                log.info("INIT InsightPiece does not exist. insightId={}", insight.getId());
                insightOutbox.markFailed();
                continue;
            }

            documents.add(InsightDocument.from(insight, insightPiece.get()));
            resolvedOutboxes.add(insightOutbox);
        }

        elasticsearchInsightService.saveAll(documents);
        log.info("Outbox fallback relay 완료: {}건 재시도 처리", resolvedOutboxes.size());

        resolvedOutboxes.forEach(InsightOutbox::markCompleted);
    }
}
