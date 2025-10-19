package com.simplecoder.tool;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ReadFileToolTest {

    @TempDir
    Path tempDir;

    private ReadFileTool tool;
    private PathValidator pathValidator;

    @BeforeEach
    void setUp() {
        pathValidator = new PathValidator(tempDir.toString());
        tool = new ReadFileTool(pathValidator, 500); // max 500 lines
    }

    @Test
    void testGetName() {
        assertEquals("read", tool.getName());
    }

    @Test
    void testReadEntireFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "line1\nline2\nline3");

        ToolRequest request = ToolRequest.builder()
                .prompt("Read test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Read test.txt"));
        assertTrue(response.getMessage().contains("lines 1-3 of 3 total"));

        String content = (String) response.getData();
        assertTrue(content.contains("1 | line1"));
        assertTrue(content.contains("2 | line2"));
        assertTrue(content.contains("3 | line3"));
    }

    @Test
    void testReadWithLineRange() throws IOException {
        Path testFile = tempDir.resolve("multi.txt");
        String content = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> "Line " + i)
                .collect(Collectors.joining("\n"));
        Files.writeString(testFile, content);

        ToolRequest request = ToolRequest.builder()
                .prompt("Read multi.txt:3-7")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("lines 3-7 of 10 total"));

        String data = (String) response.getData();
        assertTrue(data.contains("3 | Line 3"));
        assertTrue(data.contains("7 | Line 7"));
        assertFalse(data.contains("Line 2"));
        assertFalse(data.contains("Line 8"));
    }

    @Test
    void testReadFileNotFound() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Read nonexistent.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("File not found"));
    }

    @Test
    void testReadDirectory() throws IOException {
        Path dir = tempDir.resolve("subdir");
        Files.createDirectory(dir);

        ToolRequest request = ToolRequest.builder()
                .prompt("Read subdir")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("not a regular file"));
    }

    @Test
    void testReadPathOutsideRepo() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Read ../../etc/passwd")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Security error"));
    }

    @Test
    void testReadWithMaxLinesLimit() throws IOException {
        // Create file with 600 lines (exceeds max of 500)
        Path largeFile = tempDir.resolve("large.txt");
        String content = IntStream.rangeClosed(1, 600)
                .mapToObj(i -> "Line " + i)
                .collect(Collectors.joining("\n"));
        Files.writeString(largeFile, content);

        ToolRequest request = ToolRequest.builder()
                .prompt("Read large.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("TRUNCATED"));
        assertTrue(response.getMessage().contains("showing first 500 lines"));

        String data = (String) response.getData();
        assertTrue(data.contains("500 | Line 500"));
        assertFalse(data.contains("Line 501"));
    }

    @Test
    void testReadRangeExceedingFileLength() throws IOException {
        Path testFile = tempDir.resolve("short.txt");
        Files.writeString(testFile, "line1\nline2\nline3");

        ToolRequest request = ToolRequest.builder()
                .prompt("Read short.txt:5-10")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Start line 5 exceeds file length"));
    }

    @Test
    void testReadRangeEndBeyondFileLength() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "line1\nline2\nline3\nline4\nline5");

        ToolRequest request = ToolRequest.builder()
                .prompt("Read test.txt:3-100")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("lines 3-5 of 5 total"));

        String data = (String) response.getData();
        assertTrue(data.contains("3 | line3"));
        assertTrue(data.contains("5 | line5"));
    }

    @Test
    void testReadInvalidLineRange() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Read test.txt:10-5")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("End line must be >= start line"));
    }

    @Test
    void testReadInvalidStartLine() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Read test.txt:0-5")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Line numbers must be >= 1"));
    }

    @Test
    void testReadEmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.writeString(emptyFile, "");

        ToolRequest request = ToolRequest.builder()
                .prompt("Read empty.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("empty file: 0 lines"));
    }

    @Test
    void testReadSingleLine() throws IOException {
        Path testFile = tempDir.resolve("single.txt");
        Files.writeString(testFile, "only line");

        ToolRequest request = ToolRequest.builder()
                .prompt("Read single.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        String data = (String) response.getData();
        assertTrue(data.contains("1 | only line"));
    }

    @Test
    void testReadNestedFilePath() throws IOException {
        Path nested = tempDir.resolve("a/b/c");
        Files.createDirectories(nested);
        Path testFile = nested.resolve("deep.txt");
        Files.writeString(testFile, "nested content");

        ToolRequest request = ToolRequest.builder()
                .prompt("Read a/b/c/deep.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        String data = (String) response.getData();
        assertTrue(data.contains("nested content"));
    }

    @Test
    void testReadPromptWithCaseInsensitivePrefix() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "content");

        ToolRequest request = ToolRequest.builder()
                .prompt("READ test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
    }

    @Test
    void testReadNullPrompt() {
        ToolRequest request = ToolRequest.builder()
                .prompt(null)
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Invalid request"));
    }

    @Test
    void testReadRangeWithWhitespace() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "line1\nline2\nline3\nline4\nline5");

        ToolRequest request = ToolRequest.builder()
                .prompt("  Read  test.txt:2-4  ")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("lines 2-4"));
    }

    @Test
    void testReadDuplicateLinesLineNumbersUnique() throws IOException {
        Path testFile = tempDir.resolve("dups.txt");
        Files.writeString(testFile, "same\nsame\nsame");

        ToolRequest request = ToolRequest.builder()
                .prompt("Read dups.txt")
                .build();

        ToolResponse response = tool.execute(request);
        assertTrue(response.isSuccess());
        String data = (String) response.getData();
        assertTrue(data.contains("1 | same"));
        assertTrue(data.contains("2 | same"));
        assertTrue(data.contains("3 | same"));
    }
}
