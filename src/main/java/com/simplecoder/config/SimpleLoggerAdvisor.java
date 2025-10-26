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
 * Logs ChatClient requests and responses for debugging, monitoring, and educational purposes.
 *
 * <p>This advisor intercepts all chat client calls (both sync and streaming)
 * to log comprehensive details about the LLM interaction, including:
 * <ul>
 *   <li>Complete message history (system, user, assistant, tool response messages)</li>
 *   <li>Tool call details (id, name, arguments JSON) for ReAct loop observability</li>
 *   <li>Token usage and generation metadata</li>
 * </ul>
 *
 * <p>It implements both CallAdvisor and StreamAdvisor to handle both call modes.
 *
 * <p><b>Educational Value:</b> This advisor enables students to observe the complete
 * ReAct (Reason-Act-Observe) loop by showing:
 * <ol>
 *   <li>What prompt (with full context) Spring AI sends to the LLM</li>
 *   <li>What tool calls the LLM decides to make (with exact JSON arguments)</li>
 *   <li>How the conversation evolves across multiple turns</li>
 * </ol>
 *
 * <p>Based on Spring AI documentation example from advisors.adoc, enhanced for educational transparency.
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
        log.info("=== LLM Request (Full Prompt) ===");

        var messages = request.prompt().getInstructions();
        log.info("Total messages in prompt: {}", messages.size());

        for (int i = 0; i < messages.size(); i++) {
            var message = messages.get(i);
            String content = message.getText();  // Content interface method
            log.info("Message[{}] type={}: {}",
                    i,
                    message.getMessageType(),
                    truncate(content, 500));
        }
    }

    private void logResponse(ChatClientResponse chatClientResponse) {
        log.info("=== LLM Response (Full) ===");

        var chatResponse = chatClientResponse.chatResponse();
        if (chatResponse == null) {
            log.warn("ChatResponse is null");
            return;
        }

        var results = chatResponse.getResults();
        log.info("Total generations: {}", results.size());

        for (int i = 0; i < results.size(); i++) {
            var generation = results.get(i);
            var assistantMessage = generation.getOutput();

            // Log text content
            String textContent = assistantMessage.getText();
            if (textContent != null && !textContent.isEmpty()) {
                log.info("Generation[{}] text: {}", i, truncate(textContent, 500));
            }

            // Log tool calls (critical for ReAct loop observability)
            var toolCalls = assistantMessage.getToolCalls();
            if (toolCalls != null && !toolCalls.isEmpty()) {
                log.info("Generation[{}] tool calls: {} call(s)", i, toolCalls.size());
                for (int j = 0; j < toolCalls.size(); j++) {
                    var toolCall = toolCalls.get(j);
                    log.info("  ToolCall[{}]: id='{}', name='{}', args={}",
                            j,
                            toolCall.id(),
                            toolCall.name(),
                            truncate(toolCall.arguments(), 300));
                }
            }
        }

        // Log metadata (token usage)
        var metadata = chatResponse.getMetadata();
        if (metadata != null && metadata.getUsage() != null) {
            log.info("Token usage: {}", metadata.getUsage().getTotalTokens());
        }
    }

    /**
     * Truncates a string to the specified maximum length, appending "... (truncated)" if needed.
     *
     * @param text      the text to truncate
     * @param maxLength maximum length before truncation
     * @return truncated string or original if shorter than maxLength
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "(null)";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... (truncated, total length: " + text.length() + ")";
    }
}
