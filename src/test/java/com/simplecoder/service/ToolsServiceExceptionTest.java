package com.simplecoder.service;

import com.simplecoder.exception.ValidationException;
import com.simplecoder.exception.SecurityViolationException;
import com.simplecoder.exception.SystemException;
import com.simplecoder.tool.PathValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IF-1: Exception activation in ToolsService.
 * Verifies that tools throw appropriate typed exceptions instead of returning error strings.
 */
class ToolsServiceExceptionTest {

    private ToolsService toolsService;
    private PathValidator pathValidator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        pathValidator = new PathValidator(tempDir.toString());
        toolsService = new ToolsService(pathValidator, 100, 50, 50);
    }

    @Test
    @DisplayName("IF-1b: searchText throws ValidationException for empty pattern")
    void searchTextEmptyPatternThrowsValidation() {
        Path testFile = tempDir.resolve("test.txt");
        try {
            Files.writeString(testFile, "some content");
        } catch (IOException e) {
            fail("Setup failed");
        }

        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.searchText("", "test.txt", false, false)
        );
        assertTrue(ex.getMessage().contains("pattern cannot be null or empty"));
    }

    @Test
    @DisplayName("IF-1b: searchText throws ValidationException for null pattern")
    void searchTextNullPatternThrowsValidation() {
        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.searchText(null, "test.txt", false, false)
        );
        assertTrue(ex.getMessage().contains("pattern cannot be null or empty"));
    }

    @Test
    @DisplayName("IF-1b: searchText throws ValidationException for invalid regex")
    void searchTextInvalidRegexThrowsValidation() {
        Path testFile = tempDir.resolve("test.txt");
        try {
            Files.writeString(testFile, "content");
        } catch (IOException e) {
            fail("Setup failed");
        }

        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.searchText("[invalid(regex", "test.txt", true, false)
        );
        assertTrue(ex.getMessage().contains("Invalid regex pattern"));
    }

    @Test
    @DisplayName("IF-1b: readFile throws ValidationException for negative line number")
    void readFileNegativeLineThrowsValidation() {
        Path testFile = tempDir.resolve("test.txt");
        try {
            Files.writeString(testFile, "line1\nline2\nline3");
        } catch (IOException e) {
            fail("Setup failed");
        }

        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.readFile("test.txt", -1, 5)
        );
        assertTrue(ex.getMessage().contains("Line numbers must be >= 1"));
    }

    @Test
    @DisplayName("IF-1b: readFile throws ValidationException for start > total lines")
    void readFileStartBeyondFileThrowsValidation() {
        Path testFile = tempDir.resolve("test.txt");
        try {
            Files.writeString(testFile, "line1\nline2");
        } catch (IOException e) {
            fail("Setup failed");
        }

        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.readFile("test.txt", 10, 20)
        );
        assertTrue(ex.getMessage().contains("exceeds file length"));
    }

    @Test
    @DisplayName("IF-1b: readFile throws ValidationException for end < start")
    void readFileEndBeforeStartThrowsValidation() {
        Path testFile = tempDir.resolve("test.txt");
        try {
            Files.writeString(testFile, "line1\nline2\nline3\nline4\nline5");
        } catch (IOException e) {
            fail("Setup failed");
        }

        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.readFile("test.txt", 4, 2)  // start=4, end=2 (both within file length)
        );
        assertTrue(ex.getMessage().contains("End line must be >= start line"));
    }

    @Test
    @DisplayName("IF-1b: replaceText throws ValidationException for empty old string")
    void replaceTextEmptyOldStringThrowsValidation() {
        Path testFile = tempDir.resolve("test.txt");
        try {
            Files.writeString(testFile, "content");
        } catch (IOException e) {
            fail("Setup failed");
        }

        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.replaceText("test.txt", "", "new")
        );
        assertTrue(ex.getMessage().contains("Old string cannot be null or empty"));
    }

    @Test
    @DisplayName("IF-1b: replaceText throws ValidationException for null new string")
    void replaceTextNullNewStringThrowsValidation() {
        Path testFile = tempDir.resolve("test.txt");
        try {
            Files.writeString(testFile, "content");
        } catch (IOException e) {
            fail("Setup failed");
        }

        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.replaceText("test.txt", "old", null)
        );
        assertTrue(ex.getMessage().contains("New string cannot be null"));
    }

    @Test
    @DisplayName("IF-1b: replaceText throws ValidationException when old string not found")
    void replaceTextOldStringNotFoundThrowsValidation() {
        Path testFile = tempDir.resolve("test.txt");
        try {
            Files.writeString(testFile, "some content");
        } catch (IOException e) {
            fail("Setup failed");
        }

        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.replaceText("test.txt", "nonexistent", "new")
        );
        assertTrue(ex.getMessage().contains("not found in file"));
    }

    @Test
    @DisplayName("IF-1b: replaceText throws ValidationException when old string appears multiple times")
    void replaceTextMultipleOccurrencesThrowsValidation() {
        Path testFile = tempDir.resolve("test.txt");
        try {
            Files.writeString(testFile, "word word word");
        } catch (IOException e) {
            fail("Setup failed");
        }

        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.replaceText("test.txt", "word", "new")
        );
        assertTrue(ex.getMessage().contains("appears 3 times"));
        assertTrue(ex.getMessage().contains("must be unique for safety"));
    }

    @Test
    @DisplayName("IF-1b: replaceText throws ValidationException for file not found")
    void replaceTextFileNotFoundThrowsValidation() {
        ValidationException ex = assertThrows(ValidationException.class, () -> 
            toolsService.replaceText("nonexistent.txt", "old", "new")
        );
        assertTrue(ex.getMessage().contains("File not found"));
    }

    @Test
    @DisplayName("IF-1a: PathValidator throws SecurityViolationException for path escape")
    void pathValidatorThrowsSecurityViolation() {
        SecurityViolationException ex = assertThrows(SecurityViolationException.class, () -> 
            pathValidator.validate("../../etc/passwd")
        );
        assertTrue(ex.getMessage().contains("outside repository root"));
    }

    @Test
    @DisplayName("IF-1c: readFile propagates SystemException for IO failures")
    void readFileSystemExceptionForIOFailure() {
        // Create directory instead of file to trigger IO error
        Path dir = tempDir.resolve("not-a-file");
        try {
            Files.createDirectory(dir);
        } catch (IOException e) {
            fail("Setup failed");
        }

        // This will pass PathValidator but fail when trying to read as file
        assertThrows(SystemException.class, () -> 
            toolsService.readFile("not-a-file", null, null)
        );
    }
}
