package com.dnd.ahaive.infra;

import com.dnd.ahaive.global.exception.ErrorCode;
import com.dnd.ahaive.infra.claude.exception.AiCallException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
public class ResilientAiClient implements AiClient {

    private final AiClient claudeAiClient;
    private final AiClient openAiClient;
    private final CircuitBreaker claudeCircuitBreaker;
    private final CircuitBreaker openAiCircuitBreaker;

    public ResilientAiClient(
            @Qualifier("claudeAiClient") AiClient claudeAiClient,
            @Qualifier("openAiClient") AiClient openAiClient,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.claudeAiClient = claudeAiClient;
        this.openAiClient = openAiClient;
        this.claudeCircuitBreaker = circuitBreakerFactory.create("claude-client");
        this.openAiCircuitBreaker = circuitBreakerFactory.create("openai-client");
    }

    @Override
    public String sendMessage(String prompt) {
        return claudeCircuitBreaker.run(
                () -> claudeAiClient.sendMessage(prompt),
                throwable -> {
                    log.warn("[ResilientAiClient] ClaudeAiClient 호출 실패, OpenAiClient로 폴백합니다. cause: {}",
                            throwable.getMessage());
                    return openAiCircuitBreaker.run(
                            () -> openAiClient.sendMessage(prompt),
                            fallbackThrowable -> {
                                log.error("[ResilientAiClient] OpenAiClient 폴백 호출 실패했습니다. cause: {}",
                                        fallbackThrowable.getMessage(),
                                        fallbackThrowable);
                                throw new AiCallException(ErrorCode.AI_ALL_PROVIDERS_FAILED);
                            }
                    );
                }
        );
    }
}
