package com.simplecoder.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a single entry in the conversation history.
 *
 * <p>Each entry captures:
 * - timestamp: when the interaction occurred
 * - prompt: user's input
 * - result: system's response/result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextEntry {

    /**
     * Timestamp when this entry was created.
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * User's input prompt.
     */
    private String prompt;

    /**
     * System's response or result.
     */
    private String result;

    /**
     * Creates a formatted summary of this entry for context.
     * Returns full prompt and result without truncation to provide complete context to LLM.
     *
     * @return formatted string with timestamp, prompt, and result
     */
    public String getSummary() {
        return String.format("[%s] User: %s | Result: %s",
                timestamp,
                prompt != null ? prompt : "null",
                result != null ? result : "null");
    }
}
