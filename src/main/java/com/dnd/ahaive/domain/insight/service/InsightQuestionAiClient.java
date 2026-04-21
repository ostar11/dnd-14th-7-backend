package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.question.dto.response.AiQuestionResponse;
import com.dnd.ahaive.infra.claude.ClaudeAiClient;
import com.dnd.ahaive.infra.claude.prompt.ClaudeAiPrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InsightQuestionAiClient {

    private final ClaudeAiClient aiClient;
    private final ObjectMapper objectMapper;

    public CompletableFuture<AiQuestionResponse> callAiQuestion(String initThought) {
        return CompletableFuture.supplyAsync(() ->
                aiClient.sendMessage(ClaudeAiPrompt.INIT_THOUGHT_TO_QUESTION_PROMPT(initThought)))
                .thenApply(
                        questionJson -> {
                            try {
                                return objectMapper.readValue(questionJson, AiQuestionResponse.class);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        });

    }
}
