package com.simplecoder.exception;

/**
 * Base exception for all Simple Coder agent-related exceptions.
 *
 * <p>Exception Hierarchy:
 * <pre>
 * AgentException (base)
 * ├── ValidationException        - Input validation failed
 * ├── SecurityViolationException - Path escape, security breach
 * └── SystemException            - Fatal system errors (IO, runtime)
 * </pre>
 *
 * <p>All exceptions are caught by AgentService and converted to ToolResponse.error().
 */
public class AgentException extends RuntimeException {

    public AgentException(String message) {
        super(message);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}
