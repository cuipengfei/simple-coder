package com.simplecoder.tool;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tool for reading file contents with optional line range support.
 *
 * <p>Features:
 * - Read entire file or specific line range (e.g., "file.txt:10-20")
 * - Respects max-file-lines limit from configuration
 * - Returns truncation warning if file exceeds limit
 *
 * <p>Prompt format examples:
 * - "Read file.txt"
 * - "Read src/Main.java:10-50"
 */
@Slf4j
@Component
public class ReadFileTool implements Tool {

    private static final String TOOL_NAME = "read";
    private static final Pattern LINE_RANGE_PATTERN = Pattern.compile("^(.+?):(\\d+)-(\\d+)$");

    private final PathValidator pathValidator;
    private final int maxFileLines;

    public ReadFileTool(
            PathValidator pathValidator,
            @Value("${simple-coder.max-file-lines}") int maxFileLines
    ) {
        this.pathValidator = pathValidator;
        this.maxFileLines = maxFileLines;
        log.info("ReadFileTool initialized with max-file-lines: {}", maxFileLines);
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public ToolResponse execute(ToolRequest request) {
        try {
            request.validate();

            // Parse prompt to extract file path and optional line range
            ReadRequest readRequest = parsePrompt(request.getPrompt());

            // Validate path security
            Path filePath = pathValidator.validate(readRequest.filePath);

            // Check file exists
            if (!Files.exists(filePath)) {
                return ToolResponse.error(
                        "File not found: " + readRequest.filePath,
                        "Path: " + filePath
                );
            }

            if (!Files.isRegularFile(filePath)) {
                return ToolResponse.error(
                        "Path is not a regular file: " + readRequest.filePath,
                        "Path: " + filePath
                );
            }

            // Read file lines
            List<String> allLines = Files.readAllLines(filePath);
            int totalLines = allLines.size();

            // Empty file shortcut
            if (totalLines == 0) {
                return ToolResponse.success(
                        String.format("Read %s (empty file: 0 lines)", readRequest.filePath),
                        ""
                );
            }

            // Apply line range if specified
            List<String> selectedLines;
            int startLine, endLine;

            if (readRequest.hasRange()) {
                startLine = readRequest.startLine;
                endLine = Math.min(readRequest.endLine, totalLines);

                if (startLine > totalLines) {
                    return ToolResponse.error(
                            String.format("Start line %d exceeds file length (%d lines)",
                                    startLine, totalLines)
                    );
                }

                selectedLines = allLines.subList(startLine - 1, endLine);
            } else {
                startLine = 1;
                endLine = totalLines;
                selectedLines = allLines;
            }

            // Apply max lines limit
            boolean truncated = false;
            if (selectedLines.size() > maxFileLines) {
                selectedLines = selectedLines.subList(0, maxFileLines);
                truncated = true;
            }

            // Format output with line numbers
            String content = formatWithLineNumbers(selectedLines, startLine);

            String message = String.format(
                    "Read %s (lines %d-%d of %d total)",
                    readRequest.filePath,
                    startLine,
                    startLine + selectedLines.size() - 1,
                    totalLines
            );

            if (truncated) {
                message += String.format(
                        " [TRUNCATED: showing first %d lines, %d more available]",
                        maxFileLines,
                        endLine - startLine + 1 - maxFileLines
                );
            }

            return ToolResponse.success(message, content);

        } catch (SecurityException e) {
            log.warn("Security violation in ReadFileTool: {}", e.getMessage());
            return ToolResponse.error("Security error: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error reading file: {}", e.getMessage(), e);
            return ToolResponse.error("Failed to read file", e.getMessage());
        } catch (IllegalArgumentException e) {
            return ToolResponse.error("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in ReadFileTool", e);
            return ToolResponse.error("Unexpected error", e.getMessage());
        }
    }

    /**
     * Parses the prompt to extract file path and optional line range.
     *
     * @param prompt user prompt (e.g., "Read file.txt" or "Read file.txt:10-20")
     * @return parsed read request
     */
    private ReadRequest parsePrompt(String prompt) {
        // Remove common prefixes like "Read " or "read "
        String cleaned = prompt.trim().replaceFirst("(?i)^read\\s+", "");

        // Try to match line range pattern
        Matcher matcher = LINE_RANGE_PATTERN.matcher(cleaned);
        if (matcher.matches()) {
            String filePath = matcher.group(1);
            int start = Integer.parseInt(matcher.group(2));
            int end = Integer.parseInt(matcher.group(3));

            if (start < 1) {
                throw new IllegalArgumentException("Line numbers must be >= 1");
            }
            if (end < start) {
                throw new IllegalArgumentException("End line must be >= start line");
            }

            return new ReadRequest(filePath, start, end);
        }

        // No line range, just file path
        return new ReadRequest(cleaned);
    }

    /**
     * Formats lines with line numbers for display.
     *
     * @param lines lines to format
     * @param startLineNumber starting line number
     * @return formatted string with line numbers
     */
    private String formatWithLineNumbers(List<String> lines, int startLineNumber) {
        int lineNumWidth = String.valueOf(startLineNumber + lines.size() - 1).length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            int lineNum = startLineNumber + i;
            sb.append(String.format("%" + lineNumWidth + "d | %s", lineNum, lines.get(i)));
            if (i < lines.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Internal class to hold parsed read request details.
     */
    private static class ReadRequest {
        final String filePath;
        final Integer startLine;
        final Integer endLine;

        ReadRequest(String filePath) {
            this.filePath = filePath;
            this.startLine = null;
            this.endLine = null;
        }

        ReadRequest(String filePath, int startLine, int endLine) {
            this.filePath = filePath;
            this.startLine = startLine;
            this.endLine = endLine;
        }

        boolean hasRange() {
            return startLine != null && endLine != null;
        }
    }
}
