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
 *   <li><b>Recoverable</b>: Errors that provide feedback to the agent;
 *       exception message can be used as observation in multi-step scenarios.</li>
 *   <li><b>Terminal</b>: Fatal errors requiring immediate termination;
 *       security violations, resource exhaustion, system failures.</li>
 * </ul>
 */
public class AgentException extends RuntimeException {

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
