package com.dnd.ahaive.infra.openai;

import com.dnd.ahaive.global.exception.ErrorCode;
import com.dnd.ahaive.infra.AiClient;
import com.dnd.ahaive.infra.claude.exception.AiCallException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient implements AiClient {

    private final OpenAiChatModel chatModel;

    @Override
    public String sendMessage(String prompt) {
        try {
            String response = chatModel.call(prompt);
            return response.replaceAll("```json|```", "").trim();
        } catch (Exception e) {
            log.error("[OpenAiClient] AI 호출 실패: {}", e.getMessage(), e);
            throw new AiCallException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
