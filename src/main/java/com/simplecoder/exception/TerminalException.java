package com.simplecoder.exception;

import lombok.Getter;

/**
 * Terminal exceptions that immediately abort the ReAct loop.
 *
 * <p>Characteristics:
 * <ul>
 *   <li>Loop terminates immediately when this exception is caught</li>
 *   <li>No further steps are attempted</li>
 *   <li>ToolResponse.error() is returned with termination reason</li>
 *   <li>Indicates unrecoverable errors or safety violations</li>
 * </ul>
 *
 * <p>Examples of terminal errors:
 * <ul>
 *   <li>Security violations (path escape attempts, privilege escalation)</li>
 *   <li>Step limit exceeded (prevents infinite loops)</li>
 *   <li>System failures (out of memory, critical service unavailable)</li>
 * </ul>
 *
 * <p>Educational value: Demonstrates safety boundaries and resource protection
 * in autonomous agent systems.
 */
@Getter
public class TerminalException extends AgentException {

    private final String terminationReason;

    public TerminalException(String reason, String message) {
        super(message);
        this.terminationReason = reason;
    }

    public TerminalException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.terminationReason = reason;
    }

}
