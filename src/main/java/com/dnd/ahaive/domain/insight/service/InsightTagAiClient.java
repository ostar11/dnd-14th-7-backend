package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.domain.tag.dto.response.AiTagResponse;
import com.dnd.ahaive.infra.claude.ClaudeAiClient;
import com.dnd.ahaive.infra.claude.prompt.ClaudeAiPrompt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InsightTagAiClient {

    private final ClaudeAiClient aiClient;
    private final ObjectMapper objectMapper;

    public CompletableFuture<AiTagResponse> callAiTag(String initThought) {
        return CompletableFuture.supplyAsync(() ->
                        aiClient.sendMessage(ClaudeAiPrompt.INIT_THOUGHT_TO_TAG_PROMPT(initThought)))
                .thenApply(tagJson -> {
                    try {
                        return objectMapper.readValue(tagJson, AiTagResponse.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
