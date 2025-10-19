package com.simplecoder.tool;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReplaceToolTest {

    @TempDir
    Path tempDir;

    private ReplaceTool tool;
    private PathValidator pathValidator;

    @BeforeEach
    void setUp() {
        pathValidator = new PathValidator(tempDir.toString());
        tool = new ReplaceTool(pathValidator);
    }

    @Test
    void testGetName() {
        assertEquals("replace", tool.getName());
    }

    @Test
    void testReplaceSuccess() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello World! This is a test.");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'World' with 'Universe' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Replaced 'World' with 'Universe'"));

        String newContent = Files.readString(testFile);
        assertEquals("Hello Universe! This is a test.", newContent);
    }

    @Test
    void testReplaceFileNotFound() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'old' with 'new' in nonexistent.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("File not found"));
    }

    @Test
    void testReplaceOldStringNotFound() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello World");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'Universe' with 'Cosmos' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("not found in file"));

        // File should be unchanged
        String content = Files.readString(testFile);
        assertEquals("Hello World", content);
    }

    @Test
    void testReplaceMultipleOccurrences() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "foo bar foo baz");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'foo' with 'qux' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("appears 2 times"));
        assertTrue(response.getMessage().contains("must be unique"));

        // File should be unchanged
        String content = Files.readString(testFile);
        assertEquals("foo bar foo baz", content);
    }

    @Test
    void testReplaceOldEqualsNew() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello World");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'World' with 'World' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("identical"));
    }

    @Test
    void testReplaceWithEmptyString() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello DELETE World");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'DELETE ' with '' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        String newContent = Files.readString(testFile);
        assertEquals("Hello World", newContent);
    }

    @Test
    void testReplaceMultilineString() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String original = "line1\nold block\nline3";
        Files.writeString(testFile, original);

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'old block' with 'new block' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        String newContent = Files.readString(testFile);
        assertEquals("line1\nnew block\nline3", newContent);
    }

    @Test
    void testReplacePathOutsideRepo() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'old' with 'new' in ../../etc/passwd")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Security error"));
    }

    @Test
    void testReplaceDirectory() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'old' with 'new' in subdir")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("not a regular file"));
    }

    @Test
    void testReplaceInvalidPromptFormat() {
        ToolRequest request = ToolRequest.builder()
                .prompt("invalid format")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Invalid replace prompt format"));
    }

    @Test
    void testReplaceNullPrompt() {
        ToolRequest request = ToolRequest.builder()
                .prompt(null)
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Invalid request"));
    }

    @Test
    void testReplaceWithSpecialCharacters() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Price: $100.00");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace '$100.00' with '$200.00' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        String newContent = Files.readString(testFile);
        assertEquals("Price: $200.00", newContent);
    }

    @Test
    void testReplaceWithQuotes() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Say 'hello'");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace \"'hello'\" with \"'goodbye'\" in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        String newContent = Files.readString(testFile);
        assertEquals("Say 'goodbye'", newContent);
    }

    @Test
    void testReplaceLongStringsAreTruncatedInMessage() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        String longOld = "A".repeat(50);
        String longNew = "B".repeat(50);
        Files.writeString(testFile, "prefix " + longOld + " suffix");

        ToolRequest request = ToolRequest.builder()
                .prompt(String.format("Replace '%s' with '%s' in test.txt", longOld, longNew))
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        // Message should truncate long strings with "..."
        assertTrue(response.getMessage().contains("..."));

        // File should have full replacement
        String newContent = Files.readString(testFile);
        assertEquals("prefix " + longNew + " suffix", newContent);
    }

    @Test
    void testReplaceNestedFilePath() throws IOException {
        Path nested = tempDir.resolve("a/b/c");
        Files.createDirectories(nested);
        Path testFile = nested.resolve("deep.txt");
        Files.writeString(testFile, "replace me");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'replace me' with 'replaced' in a/b/c/deep.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        String newContent = Files.readString(testFile);
        assertEquals("replaced", newContent);
    }

    @Test
    void testReplaceEmptyFile() throws IOException {
        Path testFile = tempDir.resolve("empty.txt");
        Files.writeString(testFile, "");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'old' with 'new' in empty.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("not found in file"));
    }

    @Test
    void testReplaceCaseInsensitiveMatch() throws IOException {
        // ReplaceTool should be case-sensitive
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "HELLO hello");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'hello' with 'hi' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        // Only lowercase 'hello' should be replaced
        String newContent = Files.readString(testFile);
        assertEquals("HELLO hi", newContent);
    }

    @Test
    void testReplaceWithSingleQuotes() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "old text");

        ToolRequest request = ToolRequest.builder()
                .prompt("Replace 'old text' with 'new text' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        String newContent = Files.readString(testFile);
        assertEquals("new text", newContent);
    }
}
