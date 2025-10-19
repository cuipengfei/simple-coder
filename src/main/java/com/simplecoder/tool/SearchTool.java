package com.simplecoder.tool;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Tool for searching text in files with regex support.
 *
 * <p>Features:
 * - Search for text (literal or regex) in files
 * - Supports case-sensitive/insensitive search
 * - Returns results as "file:line:snippet" format
 * - Limited to max-search-results (configured)
 *
 * <p>Prompt format examples:
 * - "Search 'Agent' in docs/"
 * - "Search regex 'foo.*bar' in src/"
 * - "Search case-sensitive 'TODO' in src/main"
 */
@Slf4j
@Component
public class SearchTool implements Tool {

    private static final String TOOL_NAME = "search";
    private static final Pattern PROMPT_PATTERN = Pattern.compile(
            "(?i)^search\\s+(?:(regex|case-sensitive)\\s+)?['\"]([^'\"]+)['\"]\\s+in\\s+(.+)$"
    );

    private final PathValidator pathValidator;
    private final int maxSearchResults;

    public SearchTool(
            PathValidator pathValidator,
            @Value("${simple-coder.max-search-results}") int maxSearchResults
    ) {
        this.pathValidator = pathValidator;
        this.maxSearchResults = maxSearchResults;
        log.info("SearchTool initialized with max-search-results: {}", maxSearchResults);
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public ToolResponse execute(ToolRequest request) {
        try {
            request.validate();

            SearchRequest searchRequest = parsePrompt(request.getPrompt());
            Path repoRoot = pathValidator.getRepoRoot();

            // Validate path security
            Path searchPath = pathValidator.validate(searchRequest.path);

            if (!Files.exists(searchPath)) {
                return ToolResponse.error("Path not found: " + searchRequest.path);
            }

            List<String> results = new ArrayList<>();
            Pattern searchPattern = compilePattern(searchRequest);
            boolean truncated = false;

            if (Files.isRegularFile(searchPath)) {
                truncated = searchInFile(searchPath, searchPattern, repoRoot, results);
            } else if (Files.isDirectory(searchPath)) {
                truncated = searchInDirectoryEarlyStop(searchPath, searchPattern, repoRoot, results);
            } else {
                return ToolResponse.error("Path is neither file nor directory: " + searchRequest.path);
            }

            if (results.size() > maxSearchResults) {
                // Should not exceed, but hard guard
                results = results.subList(0, maxSearchResults);
                truncated = true;
            } else if (truncated) {
                // truncated already true from early stop
            }

            String message = String.format(
                    "Found %d matches for '%s' in %s",
                    results.size(),
                    searchRequest.searchText,
                    searchRequest.path
            );

            if (truncated) {
                message += String.format(" [TRUNCATED: reached limit %d before completing search]", maxSearchResults);
            }

            return ToolResponse.success(message, results);

        } catch (SecurityException e) {
            log.warn("Security violation in SearchTool: {}", e.getMessage());
            return ToolResponse.error("Security error: " + e.getMessage());
        } catch (PatternSyntaxException e) {
            return ToolResponse.error("Invalid regex pattern", e.getMessage());
        } catch (IOException e) {
            log.error("IO error during search: {}", e.getMessage(), e);
            return ToolResponse.error("Search failed", e.getMessage());
        } catch (IllegalArgumentException e) {
            return ToolResponse.error("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in SearchTool", e);
            return ToolResponse.error("Unexpected error", e.getMessage());
        }
    }

    private SearchRequest parsePrompt(String prompt) {
        Matcher matcher = PROMPT_PATTERN.matcher(prompt.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid search prompt format. Expected: Search ['regex'|'case-sensitive'] 'pattern' in path"
            );
        }

        String modifier = matcher.group(1); // regex or case-sensitive
        String searchText = matcher.group(2);
        String path = matcher.group(3);

        boolean isRegex = modifier != null && modifier.equalsIgnoreCase("regex");
        boolean caseSensitive = modifier != null && modifier.equalsIgnoreCase("case-sensitive");

        return new SearchRequest(searchText, path, isRegex, caseSensitive);
    }

    private Pattern compilePattern(SearchRequest request) {
        int flags = request.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;

        if (request.isRegex) {
            return Pattern.compile(request.searchText, flags);
        } else {
            // Literal search - escape regex special characters
            String escaped = Pattern.quote(request.searchText);
            return Pattern.compile(escaped, flags);
        }
    }

    private boolean searchInFile(Path file, Pattern pattern, Path repoRoot, List<String> results) throws IOException {
        // If global limit already reached before processing this file, treat as truncated context.
        if (results.size() >= maxSearchResults) {
            return true;
        }
        List<String> lines = Files.readAllLines(file);
        String relativePath = repoRoot.relativize(file).toString().replace('\\', '/');

        for (int i = 0; i < lines.size(); i++) {
            if (results.size() >= maxSearchResults) {
                // We hit the limit before finishing this file -> truncated
                return true;
            }
            String line = lines.get(i);
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                int lineNumber = i + 1;
                String snippet = line.trim();
                if (snippet.length() > 100) {
                    snippet = snippet.substring(0, 100) + "...";
                }
                results.add(String.format("%s:%d:%s", relativePath, lineNumber, snippet));
            }
        }
        // Finished scanning entire file without hitting limit
        return false;
    }

    private boolean searchInDirectoryEarlyStop(Path dir, Pattern pattern, Path repoRoot, List<String> results) throws IOException {
        try (var stream = Files.walk(dir)) {
            for (Path file : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                if (results.size() >= maxSearchResults) {
                    return true; // already reached before processing next file
                }
                boolean fileTruncated = false;
                try {
                    fileTruncated = searchInFile(file, pattern, repoRoot, results);
                } catch (IOException e) {
                    log.debug("Failed to search file: {}", file, e);
                }
                if (fileTruncated) {
                    return true; // truncated inside file
                }
                // If results.size() == maxSearchResults here, we finished this file exactly at the limit.
                // Do NOT mark truncated yet; next loop iteration will decide if there are remaining files (early stop) or exit cleanly.
            }
        }
        // Completed full traversal without early stop; reaching exactly the limit after final file is NOT considered truncated.
        return false;
    }

    private static class SearchRequest {
        final String searchText;
        final String path;
        final boolean isRegex;
        final boolean caseSensitive;

        SearchRequest(String searchText, String path, boolean isRegex, boolean caseSensitive) {
            this.searchText = searchText;
            this.path = path;
            this.isRegex = isRegex;
            this.caseSensitive = caseSensitive;
        }
    }
}
