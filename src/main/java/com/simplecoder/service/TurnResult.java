package com.simplecoder.service;

/**
 * Execution result for a single turn including tool metadata.
 * 
 * @param content The textual result/summary
 * @param toolName Name of the tool that was executed (e.g., "readFile"), or null if no tool was called
 */
public record TurnResult(String content, String toolName) {
    
    /**
     * Create a result for a turn without tool execution.
     */
    public static TurnResult withoutTool(String content) {
        return new TurnResult(content, null);
    }
    
    /**
     * Create a result for a turn with tool execution.
     */
    public static TurnResult withTool(String content, String toolName) {
        return new TurnResult(content, toolName != null && !toolName.isBlank() ? toolName : null);
    }
}
