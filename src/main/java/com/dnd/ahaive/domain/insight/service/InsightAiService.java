package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.insight.service.dto.AiInsightResponse;
import com.dnd.ahaive.domain.question.dto.response.AiQuestionResponse;
import com.dnd.ahaive.domain.tag.dto.response.AiTagResponse;
import com.dnd.ahaive.global.exception.ErrorCode;
import com.dnd.ahaive.infra.AiClient;
import com.dnd.ahaive.infra.claude.exception.AiResponseParseException;
import com.dnd.ahaive.infra.claude.prompt.ClaudeAiPrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InsightAiService {

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public AiInsightResponse generateInsightData(String initThought) {
        CompletableFuture<String> titleFuture = CompletableFuture.supplyAsync(() ->
                aiClient.sendMessage(ClaudeAiPrompt.INIT_THOUGHT_TO_TITLE_PROMPT(initThought)));

        CompletableFuture<String> pieceFuture = CompletableFuture.supplyAsync(() ->
                aiClient.sendMessage(ClaudeAiPrompt.INIT_THOUGHT_TO_INSIGHT_PROMPT(initThought)));

        CompletableFuture<AiTagResponse> tagFuture = CompletableFuture.supplyAsync(() ->
                parse(aiClient.sendMessage(ClaudeAiPrompt.INIT_THOUGHT_TO_TAG_PROMPT(initThought)), AiTagResponse.class));

        CompletableFuture<AiQuestionResponse> questionFuture = CompletableFuture.supplyAsync(() ->
                parse(aiClient.sendMessage(ClaudeAiPrompt.INIT_THOUGHT_TO_QUESTION_PROMPT(initThought)), AiQuestionResponse.class));

        CompletableFuture.allOf(titleFuture, pieceFuture, tagFuture, questionFuture).join();

        return new AiInsightResponse(
                titleFuture.join(),
                pieceFuture.join(),
                tagFuture.join(),
                questionFuture.join()
        );
    }

    private <T> T parse(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new AiResponseParseException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }
    }
}
