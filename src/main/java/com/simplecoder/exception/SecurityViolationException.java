package com.simplecoder.exception;

import lombok.Getter;

/**
 * Security violation detected - terminates loop immediately.
 *
 * <p>Thrown when security boundaries are violated. Loop must terminate to prevent
 * potential harm.
 *
 * <p>Examples:
 * <ul>
 *   <li>Path escape attempt (PathValidator detects path outside repo root)</li>
 *   <li>Attempt to access prohibited system resources</li>
 *   <li>Invalid or malicious input patterns</li>
 * </ul>
 *
 * <p>Current usage: PathValidator throws SecurityException (Java standard).
 * This class provides a typed alternative for future ReAct loop integration.
 */
@Getter
public class SecurityViolationException extends TerminalException {

    private final String violationType;

    public SecurityViolationException(String violationType, String message) {
        super("SECURITY_VIOLATION", message);
        this.violationType = violationType;
    }
}
