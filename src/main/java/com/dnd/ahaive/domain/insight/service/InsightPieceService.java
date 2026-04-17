package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightGenerationType;
import com.dnd.ahaive.domain.insight.entity.InsightPiece;
import com.dnd.ahaive.domain.insight.repository.InsightPieceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsightPieceService {

    private final InsightPieceRepository insightPieceRepository;

    @Transactional
    public void saveInsightPieces(Insight insight, String insightPieceContent) {
        InsightPiece insightPiece = InsightPiece.of(insight, insightPieceContent, InsightGenerationType.INIT);
        insightPieceRepository.save(insightPiece);
    }
}
