package com.simplecoder.tool;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool for precise string replacement in files.
 *
 * <p>Features:
 * - Exact string replacement (old_string → new_string)
 * - **Enforces unique match**: old_string must appear exactly once
 * - Multiple matches or no matches → error
 *
 * <p>Prompt format examples:
 * - "Replace 'old text' with 'new text' in file.txt"
 */
@Slf4j
@Component
public class ReplaceTool implements Tool {

    private static final String TOOL_NAME = "replace";
    private static final Pattern PROMPT_PATTERN = Pattern.compile(
            "(?i)^replace\\s+(['\"])(.*?)\\1\\s+with\\s+(['\"])(.*?)\\3\\s+in\\s+(.+)$"
    );

    private final PathValidator pathValidator;

    public ReplaceTool(PathValidator pathValidator) {
        this.pathValidator = pathValidator;
        log.info("ReplaceTool initialized");
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public ToolResponse execute(ToolRequest request) {
        try {
            request.validate();

            ReplaceRequest replaceRequest = parsePrompt(request.getPrompt());

            // Validate path security
            Path filePath = pathValidator.validate(replaceRequest.filePath);

            if (!Files.exists(filePath)) {
                return ToolResponse.error("File not found: " + replaceRequest.filePath);
            }

            if (!Files.isRegularFile(filePath)) {
                return ToolResponse.error("Path is not a regular file: " + replaceRequest.filePath);
            }

            // Validate old_string != new_string
            if (replaceRequest.oldString.equals(replaceRequest.newString)) {
                return ToolResponse.error("Old string and new string are identical - no replacement needed");
            }

            // Read file content
            String content = Files.readString(filePath);

            // Count occurrences of old_string
            int occurrences = countOccurrences(content, replaceRequest.oldString);

            if (occurrences == 0) {
                return ToolResponse.error(
                        String.format("Old string '%s' not found in file", replaceRequest.oldString)
                );
            }

            if (occurrences > 1) {
                return ToolResponse.error(
                        String.format(
                                "Old string '%s' appears %d times (must be unique for safety)",
                                replaceRequest.oldString,
                                occurrences
                        )
                );
            }

            // Perform replacement (exactly one occurrence)
            String newContent = content.replace(replaceRequest.oldString, replaceRequest.newString);

            // Write back to file
            Files.writeString(filePath, newContent);

            String message = String.format(
                    "Replaced '%s' with '%s' in %s",
                    truncateForDisplay(replaceRequest.oldString, 30),
                    truncateForDisplay(replaceRequest.newString, 30),
                    replaceRequest.filePath
            );

            log.info("Successful replacement in {}", filePath);
            return ToolResponse.success(message);

        } catch (SecurityException e) {
            log.warn("Security violation in ReplaceTool: {}", e.getMessage());
            return ToolResponse.error("Security error: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error during replacement: {}", e.getMessage(), e);
            return ToolResponse.error("Failed to replace", e.getMessage());
        } catch (IllegalArgumentException e) {
            return ToolResponse.error("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in ReplaceTool", e);
            return ToolResponse.error("Unexpected error", e.getMessage());
        }
    }

    private ReplaceRequest parsePrompt(String prompt) {
        Matcher matcher = PROMPT_PATTERN.matcher(prompt.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid replace prompt format. Expected: Replace 'old' with 'new' in file"
            );
        }

        String oldString = matcher.group(2);
        String newString = matcher.group(4);
        String filePath = matcher.group(5);

        return new ReplaceRequest(oldString, newString, filePath);
    }

    private int countOccurrences(String content, String target) {
        int count = 0;
        int index = 0;

        while ((index = content.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }

        return count;
    }

    private String truncateForDisplay(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private static class ReplaceRequest {
        final String oldString;
        final String newString;
        final String filePath;

        ReplaceRequest(String oldString, String newString, String filePath) {
            this.oldString = oldString;
            this.newString = newString;
            this.filePath = filePath;
        }
    }
}
