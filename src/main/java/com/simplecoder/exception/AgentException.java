package com.simplecoder.exception;

/**
 * Base exception for all Simple Coder agent-related exceptions.
 *
 * <p>Exception Taxonomy Design:
 * <pre>
 * AgentException (base)
 * ├── RecoverableException (continue loop with observation)
 * │   ├── ToolExecutionException     - Tool failed but loop can continue
 * │   ├── ValidationException        - Input validation failed
 * │   └── ResourceLimitException     - Hit resource limit (e.g., max search results)
 * └── TerminalException (abort loop immediately)
 *     ├── SecurityViolationException - Path escape, security breach
 *     ├── StepLimitExceededException - Max ReAct steps exceeded
 *     └── SystemException            - Fatal system errors
 * </pre>
 *
 * <p>Design Rationale:
 * <ul>
 *   <li><b>Recoverable</b>: Errors that provide feedback to the agent; loop continues,
 *       exception message becomes an observation for next reasoning step.</li>
 *   <li><b>Terminal</b>: Fatal errors requiring immediate loop termination;
 *       security violations, resource exhaustion, infinite loop prevention.</li>
 * </ul>
 *
 * <p>Usage in ReAct Loop (AgentService):
 * <pre>{@code
 * try {
 *     String result = executeTool(...);
 *     steps.add(new ExecutionStep(..., result));
 * } catch (RecoverableException e) {
 *     // Log error as observation, continue to next step
 *     steps.add(new ExecutionStep(..., "Error: " + e.getMessage()));
 *     continue; // loop continues
 * } catch (TerminalException e) {
 *     // Abort loop immediately
 *     return ToolResponse.error(e.getTerminationReason(), e.getMessage());
 * }
 * }</pre>
 */
public class AgentException extends RuntimeException {

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
