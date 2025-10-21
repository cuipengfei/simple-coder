package com.simplecoder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a user request to the coding agent.
 *
 * <p>Stateless design: Client maintains conversation history and sends it with each request.
 * - prompt: user's natural language request
 * - toolType: explicit tool selection (optional, "auto" delegates to LLM)
 * - contextHistory: previous conversation context entries (client-managed)
 *
 * <p>Naming: While "ToolRequest/ToolResponse" focuses on the tool-calling aspect,
 * renaming to "AgentRequest/AgentResponse" would better reflect the broader agent concept.
 * However, current naming is retained to avoid serialization/frontend compatibility impact.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolRequest {

    /**
     * User's natural language prompt/request.
     */
    private String prompt;

    /**
     * Tool type to use: "auto", "read", "list", "search", "replace".
     * If "auto", the LLM will select the appropriate tool.
     */
    @Builder.Default
    private String toolType = "auto";

    /**
     * Previous conversation context entries (client-managed).
     * Server is stateless; client sends full history with each request.
     */
    private List<ContextEntry> contextHistory;

    /**
     * Validates that the request has required fields.
     *
     * @throws IllegalArgumentException if prompt is null or empty
     */
    public void validate() {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        if (toolType == null || toolType.trim().isEmpty()) {
            throw new IllegalArgumentException("ToolType cannot be null or empty");
        }
    }

    /**
     * Builds a formatted context summary from the conversation history.
     * Used to provide context to the LLM during processing.
     *
     * @return formatted context string, or "No previous context." if history is empty
     */
    public String buildContextSummary() {
        if (contextHistory == null || contextHistory.isEmpty()) {
            return "No previous context.";
        }
        return contextHistory.stream()
                .map(ContextEntry::getSummary)
                .collect(Collectors.joining("\n", "--- Recent Context ---\n", "\n--- End Context ---"));
    }
}
