package com.simplecoder.exception;

/**
 * Fatal system error.
 *
 * <p>Thrown for critical system failures.
 *
 * <p>Examples:
 * <ul>
 *   <li>IO errors during file operations</li>
 *   <li>Unexpected runtime exceptions</li>
 * </ul>
 */
public class SystemException extends AgentException {

    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
