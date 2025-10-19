package com.simplecoder.service;

import com.simplecoder.model.ToolRequest;
import java.util.Set;

/**
 * Strategy interface for selecting which Tool to execute when toolType == "auto".
 *
 * Implementations (e.g. ModelToolSelectionStrategy) should:
 * 1. Use ToolRequest.buildContextSummary() + current prompt to form a classification input.
 * 2. Ask the model (or other heuristic) to return ONLY one of: read | list | search | replace.
 * 3. Sanitize and normalize the returned value (trim, toLowerCase) and validate membership.
 * 4. On invalid / ambiguous output, either fall back to a safe default (e.g. read) or throw IllegalArgumentException.
 *
 * This interface is intentionally minimal to keep the service stateless and pluggable.
 */
public interface ToolSelectionStrategy {

    Set<String> ALLOWED = Set.of("read", "list", "search", "replace");

    /**
     * Select a tool name for the provided request.
     * MUST return one of the allowed tool names defined in ALLOWED.
     *
     * @param request incoming ToolRequest (already validated by caller)
     * @return tool name ("read", "list", "search", or "replace")
     * @throws IllegalArgumentException if selection cannot be determined
     */
    String selectTool(ToolRequest request);

    /**
     * Utility default method to validate a candidate tool name.
     * @param name candidate tool name (normalized lowercase)
     * @return the same name if valid
     * @throws IllegalArgumentException if not in ALLOWED
     */
    default String validateName(String name) {
        if (!ALLOWED.contains(name)) {
            throw new IllegalArgumentException("Invalid tool selection: " + name);
        }
        return name;
    }
}
