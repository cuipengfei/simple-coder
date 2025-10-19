package com.simplecoder.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathValidatorTest {

    @TempDir
    Path tempDir;

    private PathValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PathValidator(tempDir.toString());
    }

    @Test
    void testValidateNullPath() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate(null)
        );
        assertEquals("Path cannot be null or empty", exception.getMessage());
    }

    @Test
    void testValidateEmptyPath() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validate("   ")
        );
        assertEquals("Path cannot be null or empty", exception.getMessage());
    }

    @Test
    void testValidateRelativePath() throws IOException {
        // Create a test file
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "content");

        Path result = validator.validate("test.txt");

        assertEquals(testFile, result);
        assertTrue(result.isAbsolute());
    }

    @Test
    void testValidateAbsolutePathWithinRepo() throws IOException {
        Path testFile = tempDir.resolve("file.txt");
        Files.writeString(testFile, "content");

        Path result = validator.validate(testFile.toString());

        assertEquals(testFile, result);
    }

    @Test
    void testValidateAbsolutePathOutsideRepo() {
        // Path outside temp directory
        Path outsidePath = tempDir.getParent().resolve("outside.txt");

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> validator.validate(outsidePath.toString())
        );
        assertTrue(exception.getMessage().contains("outside repository root"));
    }

    @Test
    void testValidatePathTraversalAttack() {
        // Attempt to escape using ../
        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> validator.validate("../../etc/passwd")
        );
        assertTrue(exception.getMessage().contains("outside repository root"));
    }

    @Test
    void testValidateNestedRelativePath() throws IOException {
        // Create nested directory structure
        Path nested = tempDir.resolve("a/b/c");
        Files.createDirectories(nested);
        Path testFile = nested.resolve("file.txt");
        Files.writeString(testFile, "content");

        Path result = validator.validate("a/b/c/file.txt");

        assertEquals(testFile, result);
    }

    @Test
    void testValidatePathWithDotSegments() throws IOException {
        // Path with ./  segments
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "content");

        Path result = validator.validate("./test.txt");

        assertEquals(testFile, result);
    }

    @Test
    void testValidatePathWithNormalization() throws IOException {
        // Path like a/../b should normalize to b
        Path dirA = tempDir.resolve("a");
        Path dirB = tempDir.resolve("b");
        Files.createDirectories(dirA);
        Files.createDirectories(dirB);
        Path testFile = dirB.resolve("file.txt");
        Files.writeString(testFile, "content");

        Path result = validator.validate("a/../b/file.txt");

        assertEquals(testFile, result);
    }

    @Test
    void testValidateNonExistentPath() {
        // Non-existent paths should still be validated (for create operations)
        Path result = validator.validate("nonexistent.txt");

        Path expected = tempDir.resolve("nonexistent.txt").normalize();
        assertEquals(expected, result);
        assertTrue(result.startsWith(tempDir));
    }

    @Test
    void testValidateComplexTraversalAttempt() {
        // Multiple ../ attempts
        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> validator.validate("a/../../../../../../etc/passwd")
        );
        assertTrue(exception.getMessage().contains("outside repository root"));
    }

    @Test
    void testGetRepoRoot() {
        Path root = validator.getRepoRoot();

        assertEquals(tempDir.toAbsolutePath().normalize(), root);
        assertTrue(root.isAbsolute());
    }

    @Test
    void testValidateWindowsAbsolutePath() {
        // This test assumes we're on Windows or the path format is recognized
        // Skip if path format not applicable
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            SecurityException exception = assertThrows(
                    SecurityException.class,
                    () -> validator.validate("C:\\Windows\\System32\\config")
            );
            assertTrue(exception.getMessage().contains("outside repository root"));
        }
    }

    @Test
    void testValidateSubdirectoryWithSameName() throws IOException {
        // Edge case: directory name contains parent path name
        Path subDir = tempDir.resolve("sub");
        Files.createDirectories(subDir);
        Path testFile = subDir.resolve("file.txt");
        Files.writeString(testFile, "content");

        Path result = validator.validate("sub/file.txt");

        assertEquals(testFile, result);
    }
}
