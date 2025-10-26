package com.simplecoder.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * Logs ChatClient requests and responses for debugging and monitoring.
 *
 * <p>This advisor intercepts all chat client calls (both sync and streaming)
 * to log the raw request sent to the LLM and the response received.
 * It implements both CallAdvisor and StreamAdvisor to handle both call modes.
 *
 * <p>Based on Spring AI documentation example from advisors.adoc.
 * To view logs, ensure logging level is set to INFO or DEBUG:
 * <pre>
 * logging.level.com.simplecoder.config.SimpleLoggerAdvisor=INFO
 * </pre>
 */
@Slf4j
public class SimpleLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        logRequest(chatClientRequest);
        ChatClientResponse chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        logResponse(chatClientResponse);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        logRequest(chatClientRequest);
        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnComplete(() -> log.info("=== LLM Response (Stream Completed) ==="));
    }

    private void logRequest(ChatClientRequest request) {
        log.info("=== LLM Request ===");
        if (request.prompt().getUserMessage() != null) {
            log.info("User message: {}", request.prompt().getUserMessage().getText());
        }
    }

    private void logResponse(ChatClientResponse chatClientResponse) {
        log.info("=== LLM Response ===");

        var chatResponse = chatClientResponse.chatResponse();
        if (chatResponse == null) {
            log.warn("ChatResponse is null");
            return;
        }

        var results = chatResponse.getResults();
        if (!results.isEmpty()) {
            var generation = results.get(0);
            String textContent = generation.getOutput().getText();
            if (textContent != null && !textContent.isEmpty()) {
                log.info("Content: {}", textContent.length() > 200 ?
                        textContent.substring(0, 200) + "... (truncated)" : textContent);
            }
        }

        var metadata = chatResponse.getMetadata();
        if (metadata != null && metadata.getUsage() != null) {
            log.info("Tokens: {}", metadata.getUsage().getTotalTokens());
        }
    }
}
