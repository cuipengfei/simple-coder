package com.simplecoder.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified response format for all tool operations.
 *
 * <p>According to docs/IMPLEMENTATION.md Phase 2:
 * - success: boolean indicating operation success/failure
 * - message: human-readable result or error description
 * - data: optional structured data (file content, search results, etc.)
 * - error: optional error details for debugging
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolResponse {

    /**
     * Indicates whether the operation succeeded.
     */
    private boolean success;

    /**
     * Human-readable message describing the result or error.
     */
    private String message;

    /**
     * Optional structured data (e.g., file content, search results).
     * Type depends on the tool used.
     *
     * <p>Using Object type is appropriate here because:
     * - Different tools return different data types (String for file content, List for search results, etc.)
     * - Jackson handles serialization/deserialization correctly with @JsonInclude(NON_NULL)
     * - Alternative would be generic type ToolResponse<T>, but adds complexity for minimal benefit
     */
//    todo: check if being object is ok?
    private Object data;

    /**
     * Optional error details for debugging.
     * Only present when success is false.
     */
    private String error;

    /**
     * Creates a success response with message and data.
     *
     * @param message success message
     * @param data    optional result data
     * @return ToolResponse with success=true
     */
    public static ToolResponse success(String message, Object data) {
        return ToolResponse.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Creates a success response with only a message.
     *
     * @param message success message
     * @return ToolResponse with success=true
     */
    public static ToolResponse success(String message) {
        return success(message, null);
    }

    /**
     * Creates an error response with message and optional error details.
     *
     * @param message user-friendly error message
     * @param error   detailed error information
     * @return ToolResponse with success=false
     */
    public static ToolResponse error(String message, String error) {
        return ToolResponse.builder()
                .success(false)
                .message(message)
                .error(error)
                .build();
    }

    /**
     * Creates an error response with only a message.
     *
     * @param message error message
     * @return ToolResponse with success=false
     */
    public static ToolResponse error(String message) {
        return error(message, null);
    }
}
