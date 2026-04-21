package com.dnd.ahaive.domain.insight.service;

import com.dnd.ahaive.infra.claude.ClaudeAiClient;
import com.dnd.ahaive.infra.claude.prompt.ClaudeAiPrompt;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InsightPieceAiClient {

    private final ClaudeAiClient aiClient;

    public CompletableFuture<String> callAiInsightPiece(String initThought) {
        return CompletableFuture.supplyAsync(() ->
                aiClient.sendMessage(ClaudeAiPrompt.INIT_THOUGHT_TO_INSIGHT_PROMPT(initThought)));
    }
}
