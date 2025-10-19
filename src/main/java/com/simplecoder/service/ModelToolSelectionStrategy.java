package com.simplecoder.service;

import com.simplecoder.model.ToolRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Model-backed implementation of ToolSelectionStrategy using Spring AI ChatClient.
 * Minimal GA-style usage (fluent API) without deprecated Prompt/SystemMessage/UserMessage direct construction.
 *
 * Classification Contract:
 * Returns ONLY one of: read | list | search | replace
 * Output must be a single lowercase token.
 */
@Slf4j
@Component
public class ModelToolSelectionStrategy implements ToolSelectionStrategy {

    private final ChatClient chatClient;

    public ModelToolSelectionStrategy(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String selectTool(ToolRequest request) {
        String contextSummary = request.buildContextSummary();
        String systemText = "You are a classifier that chooses exactly one tool name for a coding agent. " +
                "Return ONLY one of: read | list | search | replace. No other words.";
        String userText = buildUserInstruction(request.getPrompt(), contextSummary);

        // Fluent API call
        String raw = chatClient.prompt()
                .system(s -> s.text(systemText))
                .user(u -> u.text(userText))
                .call()
                .content();

        String normalized = normalize(raw);
        log.info("Model tool selection raw='{}' normalized='{}'", raw, normalized);
        return validateName(normalized);
    }

    private String buildUserInstruction(String currentPrompt, String contextSummary) {
        StringBuilder sb = new StringBuilder();
        if (contextSummary != null && !contextSummary.isBlank()) {
            sb.append("Context History:\n").append(contextSummary).append("\n\n");
        }
        sb.append("Current Prompt:\n").append(currentPrompt).append("\n\n");
        sb.append("Choose the best tool. Rules:\n")
          .append("read: access file contents (optionally with line range)\n")
          .append("list: list directory or glob pattern results\n")
          .append("search: find text or regex matches in files\n")
          .append("replace: perform an exact unique string replacement in a file\n\n")
          .append("Output: ONLY one of read | list | search | replace (lowercase).");
        return sb.toString();
    }

    private String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Model returned null");
        }
        String trimmed = raw.trim().toLowerCase();
        trimmed = trimmed.replaceAll("^[`\"']+|[`\"']+$", "");
        if (trimmed.contains(" ")) {
            trimmed = trimmed.split(" ")[0];
        }
        return trimmed;
    }
}
