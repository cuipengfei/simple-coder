package com.simplecoder.service;

import com.simplecoder.tool.PathValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified service containing all coding agent tools as @Tool annotated methods.
 * Spring AI ChatClient will automatically handle tool selection and parameter extraction.
 *
 * <p>Design: Each @Tool method is kept concise by delegating complex logic to focused helper methods.
 * This follows Single Responsibility Principle - tools handle orchestration and error wrapping,
 * while helpers handle specific concerns (validation, formatting, I/O operations).
 */
@Slf4j
@Service
public class ToolsService {

    private final PathValidator pathValidator;
    private final int maxFileLines;
    private final int maxListResults;
    private final int maxSearchResults;

    public ToolsService(
            PathValidator pathValidator,
            @Value("${simple-coder.max-file-lines}") int maxFileLines,
            @Value("${simple-coder.max-list-results}") int maxListResults,
            @Value("${simple-coder.max-search-results}") int maxSearchResults) {
        this.pathValidator = pathValidator;
        this.maxFileLines = maxFileLines;
        this.maxListResults = maxListResults;
        this.maxSearchResults = maxSearchResults;
        log.info("ToolsService initialized with max-file-lines={}, max-list-results={}, max-search-results={}",
                maxFileLines, maxListResults, maxSearchResults);
    }

    @Tool(description = "Read file contents, optionally with line range (e.g., lines 10-50). Returns file content with line numbers.")
    public String readFile(
            @ToolParam(description = "File path relative to repository root") String filePath,
            @ToolParam(description = "Starting line number (optional, default 1)", required = false) Integer startLine,
            @ToolParam(description = "Ending line number (optional, default end of file)", required = false) Integer endLine) {

        try {
            Path file = pathValidator.validate(filePath);
            validateFileExists(file, filePath);

            List<String> allLines = Files.readAllLines(file);
            if (allLines.isEmpty()) {
                return "Read " + filePath + " (empty file: 0 lines)";
            }

            LineRange range = validateAndParseLineRange(startLine, endLine, allLines.size());
            List<String> selectedLines = selectAndTruncateLines(allLines, range);
            String formattedContent = formatLinesWithNumbers(selectedLines, range.start());

            return buildReadFileMessage(filePath, range, allLines.size(), selectedLines.size()) + "\n\n" + formattedContent;

        } catch (SecurityException e) {
            log.warn("Security violation in readFile: {}", e.getMessage());
            return "Error: Security violation - " + e.getMessage();
        } catch (IOException e) {
            log.error("IO error reading file: {}", e.getMessage(), e);
            return "Error: Failed to read file - " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error in readFile", e);
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "List directory contents or files matching glob pattern (e.g., 'src/**/*.txt', '**/*.md'). Returns list of relative file paths.")
    public String listFiles(
            @ToolParam(description = "Directory path or glob pattern (e.g., 'src/docs' or '**/*.txt')") String path) {

        try {
            Path repoRoot = pathValidator.getRepoRoot();
            boolean isGlob = path.contains("*") || path.contains("?");

            List<String> results = isGlob ? listWithGlob(path, repoRoot) : listDirectory(path, repoRoot);
            List<String> truncatedResults = truncateResults(results, maxListResults);

            return formatListFilesMessage(path, truncatedResults.size(), results.size()) + "\n\n" + String.join("\n", truncatedResults);

        } catch (SecurityException e) {
            log.warn("Security violation in listFiles: {}", e.getMessage());
            return "Error: Security violation - " + e.getMessage();
        } catch (IOException e) {
            log.error("IO error listing files: {}", e.getMessage(), e);
            return "Error: Failed to list files - " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error in listFiles", e);
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Search for text pattern (literal or regex) in files within a directory. Returns matches with file:line:snippet format.")
    public String searchText(
            @ToolParam(description = "Text pattern to search for") String pattern,
            @ToolParam(description = "Directory or file path to search in") String searchPath,
            @ToolParam(description = "Whether pattern is regex (default false)", required = false) Boolean isRegex,
            @ToolParam(description = "Whether search is case-sensitive (default false)", required = false) Boolean caseSensitive) {

        try {
            boolean regex = isRegex != null ? isRegex : false;
            boolean caseSens = caseSensitive != null ? caseSensitive : false;

            Path path = pathValidator.validate(searchPath);
            if (!Files.exists(path)) {
                return "Error: Path not found: " + searchPath;
            }

            Path repoRoot = pathValidator.getRepoRoot();
            java.util.regex.Pattern searchPattern = compilePattern(pattern, regex, caseSens);

            List<String> results = new ArrayList<>();
            boolean truncated = false;

            if (Files.isRegularFile(path)) {
                truncated = searchInFile(path, searchPattern, repoRoot, results);
            } else if (Files.isDirectory(path)) {
                truncated = searchInDirectory(path, searchPattern, repoRoot, results);
            }

            String message = "Found " + results.size() + " matches for '" + pattern + "' in " + searchPath;
            if (truncated) {
                message += " [TRUNCATED: reached limit " + maxSearchResults + " before completing search]";
            }

            return message + "\n\n" + String.join("\n", results);

        } catch (java.util.regex.PatternSyntaxException e) {
            return "Error: Invalid regex pattern - " + e.getMessage();
        } catch (SecurityException e) {
            log.warn("Security violation in searchText: {}", e.getMessage());
            return "Error: Security violation - " + e.getMessage();
        } catch (IOException e) {
            log.error("IO error during search: {}", e.getMessage(), e);
            return "Error: Search failed - " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error in searchText", e);
            return "Error: " + e.getMessage();
        }
    }

    @Tool(description = "Replace exact string in a file. Old string must appear exactly once for safety.")
    public String replaceText(
            @ToolParam(description = "File path relative to repository root") String filePath,
            @ToolParam(description = "Old string to replace") String oldString,
            @ToolParam(description = "New string to replace with") String newString) {

        try {
            Path file = pathValidator.validate(filePath);

            if (!Files.exists(file)) {
                return "Error: File not found: " + filePath;
            }

            if (!Files.isRegularFile(file)) {
                return "Error: Path is not a regular file: " + filePath;
            }

            if (oldString.equals(newString)) {
                return "Error: Old string and new string are identical - no replacement needed";
            }

            String content = Files.readString(file);
            int occurrences = countOccurrences(content, oldString);

            if (occurrences == 0) {
                return "Error: Old string '" + truncate(oldString, 50) + "' not found in file";
            }

            if (occurrences > 1) {
                return "Error: Old string '" + truncate(oldString, 50) + "' appears " + occurrences + " times (must be unique for safety)";
            }

            String newContent = content.replace(oldString, newString);
            Files.writeString(file, newContent);

            log.info("Successful replacement in {}", file);
            return "Replaced '" + truncate(oldString, 30) + "' with '" + truncate(newString, 30) + "' in " + filePath;

        } catch (SecurityException e) {
            log.warn("Security violation in replaceText: {}", e.getMessage());
            return "Error: Security violation - " + e.getMessage();
        } catch (IOException e) {
            log.error("IO error during replacement: {}", e.getMessage(), e);
            return "Error: Failed to replace - " + e.getMessage();
        } catch (Exception e) {
            log.error("Unexpected error in replaceText", e);
            return "Error: " + e.getMessage();
        }
    }

    // Helper methods for readFile

    private record LineRange(int start, int end, boolean wasTruncated) {}

    private void validateFileExists(Path file, String filePath) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + filePath);
        }
        if (!Files.isRegularFile(file)) {
            throw new IOException("Path is not a regular file: " + filePath);
        }
    }

    private LineRange validateAndParseLineRange(Integer startLine, Integer endLine, int totalLines) throws IOException {
        int start = startLine != null ? startLine : 1;
        int end = endLine != null ? Math.min(endLine, totalLines) : totalLines;

        if (start < 1) {
            throw new IOException("Line numbers must be >= 1");
        }
        if (start > totalLines) {
            throw new IOException("Start line " + start + " exceeds file length (" + totalLines + " lines)");
        }
        if (end < start) {
            throw new IOException("End line must be >= start line");
        }

        return new LineRange(start, end, false);
    }

    private List<String> selectAndTruncateLines(List<String> allLines, LineRange range) {
        List<String> selected = allLines.subList(range.start() - 1, range.end());
        if (selected.size() > maxFileLines) {
            return selected.subList(0, maxFileLines);
        }
        return selected;
    }

    private String formatLinesWithNumbers(List<String> lines, int startLineNum) {
        StringBuilder content = new StringBuilder();
        int lineNumWidth = String.valueOf(startLineNum + lines.size() - 1).length();

        for (int i = 0; i < lines.size(); i++) {
            int lineNum = startLineNum + i;
            content.append(String.format("%" + lineNumWidth + "d | %s", lineNum, lines.get(i)));
            if (i < lines.size() - 1) {
                content.append("\n");
            }
        }
        return content.toString();
    }

    private String buildReadFileMessage(String filePath, LineRange range, int totalLines, int selectedSize) {
        String message = "Read " + filePath + " (lines " + range.start() + "-" + (range.start() + selectedSize - 1) + " of " + totalLines + " total)";

        int requestedSize = range.end() - range.start() + 1;
        if (selectedSize < requestedSize) {
            int remaining = requestedSize - selectedSize;
            message += " [TRUNCATED: showing first " + maxFileLines + " lines, " + remaining + " more available]";
        }
        return message;
    }

    // Helper methods for listFiles

    private List<String> listDirectory(String path, Path repoRoot) throws IOException {
        Path dirPath = pathValidator.validate(path);
        if (!Files.exists(dirPath)) {
            throw new IOException("Directory not found: " + path);
        }
        if (!Files.isDirectory(dirPath)) {
            throw new IOException("Path is not a directory: " + path);
        }

        List<String> results = new ArrayList<>();
        try (var stream = Files.list(dirPath)) {
            stream.forEach(p -> {
                String relativePath = repoRoot.relativize(p).toString().replace('\\', '/');
                results.add(relativePath);
            });
        }
        results.sort(String::compareTo);
        return results;
    }

    private List<String> truncateResults(List<String> results, int maxResults) {
        if (results.size() > maxResults) {
            return results.subList(0, maxResults);
        }
        return results;
    }

    private String formatListFilesMessage(String path, int displayedCount, int totalCount) {
        String message = "Found " + displayedCount + " items matching '" + path + "'";
        if (displayedCount < totalCount) {
            message += " [TRUNCATED: first " + maxListResults + " items]";
        }
        return message;
    }

    // Helper methods for glob pattern matching

    private List<String> listWithGlob(String pattern, Path repoRoot) throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        List<String> results = new ArrayList<>();

        Path startPath = extractBasePath(pattern);
        Path resolvedStart = pathValidator.validate(startPath.toString());

        Files.walkFileTree(resolvedStart, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                Path relativePath = repoRoot.relativize(file);
                String relativeStr = relativePath.toString().replace('\\', '/');

                if (matcher.matches(Paths.get(relativeStr))) {
                    results.add(relativeStr);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                Path relativePath = repoRoot.relativize(dir);
                String relativeStr = relativePath.toString().replace('\\', '/');

                if (!relativeStr.isEmpty() && matcher.matches(Paths.get(relativeStr))) {
                    results.add(relativeStr);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.debug("Failed to visit file: {}", file, exc);
                return FileVisitResult.CONTINUE;
            }
        });

        results.sort(String::compareTo);
        return results;
    }

    private Path extractBasePath(String pattern) {
        int globIndex = Math.min(
                pattern.indexOf('*') != -1 ? pattern.indexOf('*') : Integer.MAX_VALUE,
                pattern.indexOf('?') != -1 ? pattern.indexOf('?') : Integer.MAX_VALUE
        );

        if (globIndex == Integer.MAX_VALUE) {
            return Paths.get(pattern);
        }

        String beforeGlob = pattern.substring(0, globIndex);
        int lastSep = Math.max(beforeGlob.lastIndexOf('/'), beforeGlob.lastIndexOf('\\'));

        if (lastSep == -1) {
            return Paths.get(".");
        }

        return Paths.get(beforeGlob.substring(0, lastSep + 1));
    }

    private java.util.regex.Pattern compilePattern(String pattern, boolean isRegex, boolean caseSensitive) {
        int flags = caseSensitive ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;

        if (isRegex) {
            return java.util.regex.Pattern.compile(pattern, flags);
        } else {
            String escaped = java.util.regex.Pattern.quote(pattern);
            return java.util.regex.Pattern.compile(escaped, flags);
        }
    }

    private boolean searchInFile(Path file, java.util.regex.Pattern pattern, Path repoRoot, List<String> results) throws IOException {
        if (results.size() >= maxSearchResults) {
            return true;
        }

        List<String> lines = Files.readAllLines(file);
        String relativePath = repoRoot.relativize(file).toString().replace('\\', '/');

        for (int i = 0; i < lines.size(); i++) {
            if (results.size() >= maxSearchResults) {
                return true;
            }

            String line = lines.get(i);
            java.util.regex.Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                int lineNumber = i + 1;
                String snippet = line.trim();
                if (snippet.length() > 100) {
                    snippet = snippet.substring(0, 100) + "...";
                }
                results.add(String.format("%s:%d:%s", relativePath, lineNumber, snippet));
            }
        }

        return false;
    }

    private boolean searchInDirectory(Path dir, java.util.regex.Pattern pattern, Path repoRoot, List<String> results) throws IOException {
        try (var stream = Files.walk(dir)) {
            for (Path file : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                if (results.size() >= maxSearchResults) {
                    return true;
                }
                try {
                    boolean truncated = searchInFile(file, pattern, repoRoot, results);
                    if (truncated) {
                        return true;
                    }
                } catch (IOException e) {
                    log.debug("Failed to search file: {}", file, e);
                }
            }
        }
        return false;
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

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
