package com.simplecoder.tool;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool for listing directory contents and glob pattern matching.
 *
 * <p>Features:
 * - List files and directories
 * - Support glob patterns (e.g., "src/**\/*.java")
 * - Returns relative paths from repo root
 *
 * <p>Prompt format examples:
 * - "List src/"
 * - "List src/**\/*.java"
 */
@Slf4j
@Component
public class ListDirTool implements Tool {

    private static final String TOOL_NAME = "list";

    private final PathValidator pathValidator;

    private final int maxListResults;

    public ListDirTool(PathValidator pathValidator,
                       @org.springframework.beans.factory.annotation.Value("${simple-coder.max-list-results}") int maxListResults) {
        this.pathValidator = pathValidator;
        this.maxListResults = maxListResults;
        log.info("ListDirTool initialized with max-list-results: {}", maxListResults);
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public ToolResponse execute(ToolRequest request) {
        try {
            request.validate();

            String pattern = parsePrompt(request.getPrompt());
            Path repoRoot = pathValidator.getRepoRoot();

            // Validate the base path is within repo
            Path basePath = extractBasePath(pattern);
            pathValidator.validate(basePath.toString());

            // Check if pattern contains glob characters
            boolean isGlob = pattern.contains("*") || pattern.contains("?");

            List<String> results;
            if (isGlob) {
                results = listWithGlob(pattern, repoRoot);
            } else {
                results = listDirectory(basePath, repoRoot);
            }

            boolean truncated = false;
            if (results.size() > maxListResults) {
                results = results.subList(0, maxListResults);
                truncated = true;
            }

            String message = String.format(
                    "Found %d items matching '%s'%s",
                    results.size(),
                    pattern,
                    truncated ? String.format(" [TRUNCATED: first %d items]", maxListResults) : ""
            );

            return ToolResponse.success(message, results);

        } catch (SecurityException e) {
            log.warn("Security violation in ListDirTool: {}", e.getMessage());
            return ToolResponse.error("Security error: " + e.getMessage());
        } catch (NoSuchFileException e) {
            return ToolResponse.error("Directory not found: " + e.getMessage());
        } catch (IOException e) {
            log.error("IO error listing directory: {}", e.getMessage(), e);
            return ToolResponse.error("Failed to list directory", e.getMessage());
        } catch (IllegalArgumentException e) {
            return ToolResponse.error("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in ListDirTool", e);
            return ToolResponse.error("Unexpected error", e.getMessage());
        }
    }

    private String parsePrompt(String prompt) {
        return prompt.trim().replaceFirst("(?i)^list\\s+", "");
    }

    private Path extractBasePath(String pattern) {
        // Extract the base directory path before any glob characters
        int globIndex = Math.min(
                pattern.indexOf('*') != -1 ? pattern.indexOf('*') : Integer.MAX_VALUE,
                pattern.indexOf('?') != -1 ? pattern.indexOf('?') : Integer.MAX_VALUE
        );

        if (globIndex == Integer.MAX_VALUE) {
            return Paths.get(pattern);
        }

        // Find the last directory separator before glob
        String beforeGlob = pattern.substring(0, globIndex);
        int lastSep = Math.max(beforeGlob.lastIndexOf('/'), beforeGlob.lastIndexOf('\\'));

        if (lastSep == -1) {
            return Paths.get(".");
        }

        return Paths.get(beforeGlob.substring(0, lastSep + 1));
    }

    private List<String> listDirectory(Path dir, Path repoRoot) throws IOException {
        Path resolvedDir = pathValidator.validate(dir.toString());

        if (!Files.exists(resolvedDir)) {
            throw new NoSuchFileException(dir.toString());
        }

        if (!Files.isDirectory(resolvedDir)) {
            throw new IllegalArgumentException("Path is not a directory: " + dir);
        }

        List<String> results = new ArrayList<>();
        try (var stream = Files.list(resolvedDir)) {
            stream.forEach(path -> {
                String relativePath = repoRoot.relativize(path).toString();
                results.add(relativePath.replace('\\', '/'));
            });
        }

        results.sort(String::compareTo);
        return results;
    }

    private List<String> listWithGlob(String pattern, Path repoRoot) throws IOException {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        List<String> results = new ArrayList<>();

        Path startPath = pathValidator.validate(extractBasePath(pattern).toString());

        Files.walkFileTree(startPath, new SimpleFileVisitor<>() {
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
}
