package com.simplecoder.tool;

import com.simplecoder.exception.SecurityViolationException;
import lombok.Getter;
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
 * - Throws SecurityViolationException for paths outside repo
 */
@Getter
@Slf4j
@Component
public class PathValidator {

    /**
     * -- GETTER --
     * Gets the repository root path.
     *
     * @return absolute path to repository root
     */
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
     * @throws SecurityViolationException if path is outside repository root
     * @throws IllegalArgumentException   if path is null or empty
     */
    public Path validate(String pathString) {
        validateNotEmpty(pathString);
        Path resolvedPath = resolvePath(pathString);
        Path normalizedPath = resolveSymlinks(resolvedPath);
        ensureWithinRepo(normalizedPath);

        log.debug("Validated path: {} -> {}", pathString, normalizedPath);
        return normalizedPath;
    }

    private void validateNotEmpty(String pathString) {
        if (pathString == null || pathString.trim().isEmpty()) {
            throw new IllegalArgumentException("Path cannot be null or empty");
        }
    }

    private Path resolvePath(String pathString) {
        Path path = Paths.get(pathString);
        if (path.isAbsolute()) {
            return path.normalize();
        } else {
            return repoRoot.resolve(path).normalize();
        }
    }

    private Path resolveSymlinks(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            // File doesn't exist yet, use normalized path
            // This is OK for operations like create/write
            log.debug("Path does not exist (yet): {}", path);
            return path;
        }
    }

    private void ensureWithinRepo(Path path) {
        if (!path.startsWith(repoRoot)) {
            String errorMsg = String.format(
                    "Path '%s' is outside repository root '%s'",
                    path, repoRoot
            );
            log.warn("Security violation: {}", errorMsg);
            throw new SecurityViolationException("PATH_ESCAPE", errorMsg);
        }
    }

}
