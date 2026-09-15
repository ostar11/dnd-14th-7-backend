package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightGenerationType;
import com.dnd.ahaive.domain.insight.entity.InsightPiece;
import com.dnd.ahaive.domain.insight.repository.InsightPieceRepository;
import com.dnd.ahaive.domain.insight.repository.InsightRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsightDocumentReader {

    private final InsightRepository insightRepository;
    private final InsightPieceRepository insightPieceRepository;

    @Transactional
    public InsightDocumentSyncDto loadInsightDocumentRequest(Long insightId) {
        Insight insight = insightRepository.findByIdWithUser(insightId)
                .orElseThrow(() -> new EntityNotFoundException("Insight save fail, insightId=" + insightId));

        InsightPiece insightPiece = insightPieceRepository.findByInsightAndCreatedType(insight,
                        InsightGenerationType.INIT)
                .orElseThrow(() ->
                        new EntityNotFoundException("InsightPiece Entity not found. insightId=" + insightId));

        return new InsightDocumentSyncDto(insight, insightPiece);
    }
}
