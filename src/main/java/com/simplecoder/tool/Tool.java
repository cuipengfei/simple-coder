package com.simplecoder.tool;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;

/**
 * Base interface for all coding agent tools.
 *
 * <p>According to docs/IMPLEMENTATION.md Phase 3:
 * - Each tool implements a specific file operation (read/list/search/replace)
 * - Tools use PathValidator for security
 * - Returns ToolResponse with success/error status
 */
public interface Tool {

    /**
     * Gets the tool's unique identifier.
     * Used for routing and tool selection.
     *
     * @return tool name (e.g., "read", "list", "search", "replace")
     */
    String getName();

    /**
     * Executes the tool's operation based on the request.
     *
     * @param request user's tool request with prompt and context
     * @return response with success status, message, and optional data
     */
    ToolResponse execute(ToolRequest request);
}
