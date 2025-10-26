package com.simplecoder.exception;

import lombok.Getter;

/**
 * Security violation detected.
 *
 * <p>Thrown when security boundaries are violated.
 *
 * <p>Examples:
 * <ul>
 *   <li>Path escape attempt (PathValidator detects path outside repo root)</li>
 *   <li>Attempt to access prohibited system resources</li>
 *   <li>Invalid or malicious input patterns</li>
 * </ul>
 */
@Getter
public class SecurityViolationException extends AgentException {

    private final String violationType;

    public SecurityViolationException(String violationType, String message) {
        super(message);
        this.violationType = violationType;
    }
}
