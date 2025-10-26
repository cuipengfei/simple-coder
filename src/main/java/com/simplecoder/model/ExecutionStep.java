package com.simplecoder.model;

/**
 * Represents a single step in the ReAct (Reasoning-Acting-Observing) loop execution.
 *
 * <p>Each ExecutionStep captures the state of one iteration in the agent's multi-step
 * execution process, including what action was taken, which tool was used, the result,
 * and a snapshot of the task list at that point in time.
 *
 * <p>Design characteristics:
 * - Immutable by design (Java record)
 * - stepNumber: Sequential step counter (0-based or 1-based depending on implementation)
 * - actionPrompt: The natural language prompt/action for this step
 * - toolName: Which tool was selected/executed (e.g., "readFile", "searchText")
 * - resultSummary: Summary of the tool execution result
 * - tasksSnapshot: JSON or formatted string snapshot of TODO tasks at this step (nullable)
 *
 * @param stepNumber Sequential step counter in the execution loop
 * @param actionPrompt Natural language description of the action taken in this step
 * @param toolName Name of the tool that was executed (e.g., "readFile", "searchText")
 * @param resultSummary Summary of the tool execution result
 * @param tasksSnapshot Snapshot of the task list state at this step (nullable)
 */
public record ExecutionStep(
        int stepNumber,
        String actionPrompt,
        String toolName,
        String resultSummary,
        String tasksSnapshot
) {
}
