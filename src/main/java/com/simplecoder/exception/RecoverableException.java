package com.simplecoder.exception;

/**
 * Recoverable exceptions that allow the ReAct loop to continue.
 *
 * <p>Characteristics:
 * <ul>
 *   <li>Loop does NOT terminate when this exception is caught</li>
 *   <li>Exception message becomes an observation for the agent</li>
 *   <li>Agent can reason about the error and try alternative approaches</li>
 *   <li>Step counter increments normally</li>
 * </ul>
 *
 * <p>Examples of recoverable errors:
 * <ul>
 *   <li>Tool execution failures (file not found, regex syntax error)</li>
 *   <li>Validation failures (invalid line numbers, empty patterns)</li>
 *   <li>Resource limits (search results truncated, file too large)</li>
 * </ul>
 *
 * <p>Educational value: Demonstrates how agents handle partial failures
 * and adapt their strategy based on error feedback.
 */
public class RecoverableException extends AgentException {

    public RecoverableException(String message) {
        super(message);
    }

    public RecoverableException(String message, Throwable cause) {
        super(message, cause);
    }
}
