package com.simplecoder.exception;

import lombok.Getter;

/**
 * Input validation failed but loop can continue with feedback.
 *
 * <p>Thrown when tool parameters fail validation checks. Agent can adjust
 * parameters and retry.
 *
 * <p>Examples:
 * <ul>
 *   <li>Invalid line range (start > end, negative numbers)</li>
 *   <li>Empty pattern in searchText</li>
 *   <li>oldString equals newString in replaceText</li>
 * </ul>
 */
@Getter
public class ValidationException extends RecoverableException {

    private final String parameterName;

    public ValidationException(String parameterName, String message) {
        super(String.format("Validation failed for '%s': %s", parameterName, message));
        this.parameterName = parameterName;
    }

}
