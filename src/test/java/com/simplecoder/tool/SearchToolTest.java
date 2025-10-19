package com.simplecoder.tool;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchToolTest {

    @TempDir
    Path tempDir;

    private SearchTool tool;
    private PathValidator pathValidator;

    @BeforeEach
    void setUp() {
        pathValidator = new PathValidator(tempDir.toString());
        tool = new SearchTool(pathValidator, 50); // max 50 results
    }

    @Test
    void testGetName() {
        assertEquals("search", tool.getName());
    }

    @Test
    void testSearchLiteralInFile() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "line1 Agent here\nline2 nothing\nline3 Agent again");

        ToolRequest request = ToolRequest.builder()
                .prompt("Search 'Agent' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Found 2 matches"));

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(2, results.size());
        assertTrue(results.get(0).contains("test.txt:1:"));
        assertTrue(results.get(0).contains("Agent"));
        assertTrue(results.get(1).contains("test.txt:3:"));
    }

    @Test
    void testSearchCaseInsensitive() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "AGENT\nagent\nAgent\nagEnT");

        ToolRequest request = ToolRequest.builder()
                .prompt("Search 'agent' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(4, results.size()); // All 4 lines match (case-insensitive)
    }

    @Test
    void testSearchCaseSensitive() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "AGENT\nagent\nAgent");

        ToolRequest request = ToolRequest.builder()
                .prompt("Search case-sensitive 'agent' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(1, results.size()); // Only lowercase 'agent' matches
        assertTrue(results.get(0).contains(":2:"));
    }

    @Test
    void testSearchRegex() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "foo123bar\nfoobar\nfoo456bar\nbaz");

        ToolRequest request = ToolRequest.builder()
                .prompt("Search regex 'foo[0-9]+bar' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(2, results.size());
        assertTrue(results.get(0).contains("foo123bar"));
        assertTrue(results.get(1).contains("foo456bar"));
    }

    @Test
    void testSearchInDirectory() throws IOException {
        Path dir = tempDir.resolve("docs");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("file1.txt"), "Agent in file1\nno match");
        Files.writeString(dir.resolve("file2.txt"), "no match\nAgent in file2");

        ToolRequest request = ToolRequest.builder()
                .prompt("Search 'Agent' in docs")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Found 2 matches"));

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(2, results.size());
    }

    @Test
    void testSearchNoMatches() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "nothing to see here");

        ToolRequest request = ToolRequest.builder()
                .prompt("Search 'Agent' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Found 0 matches"));

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertTrue(results.isEmpty());
    }

    @Test
    void testSearchPathNotFound() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Search 'test' in nonexistent.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Path not found"));
    }

    @Test
    void testSearchPathOutsideRepo() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Search 'test' in ../../etc/passwd")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Security error"));
    }

    @Test
    void testSearchWithMaxResults() throws IOException {
        // Create file with 100 matching lines
        StringBuilder content = new StringBuilder();
        for (int i = 1; i <= 100; i++) {
            content.append("Line ").append(i).append(" with MATCH\n");
        }
        Path largeFile = tempDir.resolve("large.txt");
        Files.writeString(largeFile, content.toString());

        ToolRequest request = ToolRequest.builder()
                .prompt("Search 'MATCH' in large.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("TRUNCATED"));
        assertTrue(response.getMessage().contains("showing first 50 results"));

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(50, results.size()); // Limited to max 50
    }

    @Test
    void testSearchInvalidRegex() throws IOException {
        // Create a test file first (even though search will fail on regex)
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "some content");

        ToolRequest request = ToolRequest.builder()
                .prompt("Search regex '(' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Invalid regex pattern"));
    }

    @Test
    void testSearchLongLineIsTruncated() throws IOException {
        String longLine = "A".repeat(200) + " Agent " + "B".repeat(200);
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, longLine);

        ToolRequest request = ToolRequest.builder()
                .prompt("Search 'Agent' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(1, results.size());
        String result = results.get(0);
        assertTrue(result.contains("..."));
        assertTrue(result.length() < longLine.length());
    }

    @Test
    void testSearchInNestedDirectory() throws IOException {
        Path nested = tempDir.resolve("a/b/c");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("deep.txt"), "Agent deep");

        ToolRequest request = ToolRequest.builder()
                .prompt("Search 'Agent' in a")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(1, results.size());
        assertTrue(results.get(0).contains("a/b/c/deep.txt"));
    }

    @Test
    void testSearchInvalidPromptFormat() {
        ToolRequest request = ToolRequest.builder()
                .prompt("invalid format")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Invalid search prompt format"));
    }

    @Test
    void testSearchNullPrompt() {
        ToolRequest request = ToolRequest.builder()
                .prompt(null)
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Invalid request"));
    }

    @Test
    void testSearchWithSingleQuotes() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "find this text");

        ToolRequest request = ToolRequest.builder()
                .prompt("Search 'this' in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(1, results.size());
    }

    @Test
    void testSearchWithDoubleQuotes() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "find this text");

        ToolRequest request = ToolRequest.builder()
                .prompt("Search \"this\" in test.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(1, results.size());
    }
}
