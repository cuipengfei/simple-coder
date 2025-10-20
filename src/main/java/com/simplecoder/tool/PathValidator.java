package com.simplecoder.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Validates file paths to ensure they stay within the repository root.
 *
 * <p>Security core component - prevents path traversal attacks.
 * According to docs/IMPLEMENTATION.md § PathValidator:
 * - All file operations must be within repo-root
 * - Resolves relative paths and normalizes them
 * - Throws SecurityException for paths outside repo
 */
@Slf4j
@Component
public class PathValidator {

    private final Path repoRoot;

    public PathValidator(@Value("${simple-coder.repo-root}") String repoRootPath) {
        this.repoRoot = Paths.get(repoRootPath).toAbsolutePath().normalize();
        log.info("PathValidator initialized with repo root: {}", this.repoRoot);
    }

    /**
     * Validates and resolves a file path to ensure it's within the repository.
     *
     * @param pathString path to validate (relative or absolute)
     * @return normalized absolute Path within repo
     * @throws SecurityException        if path is outside repository root
     * @throws IllegalArgumentException if path is null or empty
     */
    public Path validate(String pathString) {
        if (pathString == null || pathString.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }

        // Convert to Path and resolve against repo root if relative
        Path path = Paths.get(pathString);
        Path resolvedPath;

        if (path.isAbsolute()) {
            resolvedPath = path.normalize();
        } else {
            resolvedPath = repoRoot.resolve(path).normalize();
        }

        // Convert to real path to resolve symlinks (if exists)
        try {
            resolvedPath = resolvedPath.toRealPath();
        } catch (IOException e) {
            // File doesn't exist yet, use normalized path
            // This is OK for operations like create/write
            log.debug("Path does not exist (yet): {}", resolvedPath);
        }

        // Security check: ensure resolved path is within repo root
        if (!resolvedPath.startsWith(repoRoot)) {
            String errorMsg = String.format(
                    "Path '%s' is outside repository root '%s'",
                    resolvedPath, repoRoot
            );
            log.warn("Security violation: {}", errorMsg);
            throw new SecurityException(errorMsg);
        }

        log.debug("Validated path: {} -> {}", pathString, resolvedPath);
        return resolvedPath;
    }

    /**
     * Gets the repository root path.
     *
     * @return absolute path to repository root
     */
    public Path getRepoRoot() {
        return repoRoot;
    }
}
