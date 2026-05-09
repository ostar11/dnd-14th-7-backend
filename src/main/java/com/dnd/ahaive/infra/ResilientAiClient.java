package com.dnd.ahaive.infra;

import com.dnd.ahaive.global.exception.ErrorCode;
import com.dnd.ahaive.infra.claude.ClaudeAiClient;
import com.dnd.ahaive.infra.claude.exception.AiCallException;
import com.dnd.ahaive.infra.openai.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class ResilientAiClient implements AiClient {

    private final ClaudeAiClient claudeAiClient;
    private final OpenAiClient openAiClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Override
    public String sendMessage(String prompt) {
        return circuitBreakerFactory.create("ai-client")
                .run(
                        () -> claudeAiClient.sendMessage(prompt),
                        throwable -> {
                            log.warn("[ResilientAiClient] ClaudeAiClient 호출 실패, OpenAiClient로 폴백합니다. cause: {}", throwable.getMessage());
                            try {
                                return openAiClient.sendMessage(prompt);
                            } catch (Exception e) {
                                log.error("[ResilientAiClient] OpenAiClient 폴백도 실패했습니다. cause: {}", e.getMessage(), e);
                                throw new AiCallException(ErrorCode.AI_ALL_PROVIDERS_FAILED);
                            }
                        }
                );
    }
}
