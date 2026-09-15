package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.entity.Insight;
import com.dnd.ahaive.domain.insight.entity.InsightPiece;

public record InsightDocumentSyncDto(Insight insight, InsightPiece insightPiece) {
}
