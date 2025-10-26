package com.simplecoder.exception;

/**
 * Fatal system error - terminates loop immediately.
 *
 * <p>Thrown for critical system failures that cannot be recovered from within
 * the ReAct loop.
 *
 * <p>Examples:
 * <ul>
 *   <li>Out of memory errors</li>
 *   <li>Critical service unavailable (ChatClient failure)</li>
 *   <li>Unexpected runtime exceptions that compromise system integrity</li>
 * </ul>
 */
public class SystemException extends TerminalException {

    public SystemException(String message) {
        super("SYSTEM_ERROR", message);
    }

    public SystemException(String message, Throwable cause) {
        super("SYSTEM_ERROR", message, cause);
    }
}
