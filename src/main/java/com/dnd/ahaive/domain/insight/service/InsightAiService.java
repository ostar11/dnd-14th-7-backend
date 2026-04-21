package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.service.dto.AiInsightResponse;
import com.dnd.ahaive.domain.question.dto.response.AiQuestionResponse;
import com.dnd.ahaive.domain.tag.dto.response.AiTagResponse;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InsightAiService {

    private final InsightTitleAiClient insightTitleAiClient;
    private final InsightPieceAiClient insightPieceAiClient;
    private final InsightTagAiClient insightTagAiClient;
    private final InsightQuestionAiClient insightQuestionAiClient;

    public AiInsightResponse createInsightAndQuestion(String initThought) {
        CompletableFuture<String> titleFuture = insightTitleAiClient.callAiTitle(initThought);
        CompletableFuture<String> insightPieceFuture = insightPieceAiClient.callAiInsightPiece(initThought);
        CompletableFuture<AiTagResponse> tagFuture = insightTagAiClient.callAiTag(initThought);
        CompletableFuture<AiQuestionResponse> questionFuture = insightQuestionAiClient.callAiQuestion(initThought);

        CompletableFuture.allOf(titleFuture, insightPieceFuture, tagFuture, questionFuture).join();

        String title = titleFuture.join();
        String insightPieceContent = insightPieceFuture.join();
        AiTagResponse aiTagResponse = tagFuture.join();
        AiQuestionResponse aiQuestionResponse = questionFuture.join();

        return new AiInsightResponse(title, insightPieceContent, aiTagResponse, aiQuestionResponse);
    }
}
