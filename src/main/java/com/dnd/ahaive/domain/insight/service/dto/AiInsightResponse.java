package com.dnd.ahaive.domain.insight.service.dto;

import com.dnd.ahaive.domain.question.dto.response.AiQuestionResponse;
import com.dnd.ahaive.domain.tag.dto.response.AiTagResponse;

public record AiInsightResponse(
        String title,
        String insightPieceContent,
        AiTagResponse aiTagResponse,
        AiQuestionResponse aiQuestionResponse
) {
}
