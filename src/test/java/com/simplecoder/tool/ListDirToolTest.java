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
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ListDirToolTest {

    @TempDir
    Path tempDir;

    private ListDirTool tool;
    private PathValidator pathValidator;

    @BeforeEach
    void setUp() {
        pathValidator = new PathValidator(tempDir.toString());
        // use small max-list-results = 10 to test truncation
        tool = new ListDirTool(pathValidator, 10);
    }

    @Test
    void testGetName() {
        assertEquals("list", tool.getName());
    }

    @Test
    void testListDirectoryNonTruncated() throws IOException {
        Path dir = tempDir.resolve("docs");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("a.txt"), "a");
        Files.writeString(dir.resolve("b.txt"), "b");

        ToolRequest request = ToolRequest.builder()
                .prompt("List docs")
                .build();

        ToolResponse response = tool.execute(request);
        assertTrue(response.isSuccess());
        assertFalse(response.getMessage().contains("TRUNCATED"));
        @SuppressWarnings("unchecked")
        List<String> data = (List<String>) response.getData();
        assertEquals(2, data.size());
    }

    @Test
    void testListDirectoryTruncated() throws IOException {
        Path dir = tempDir.resolve("bulk");
        Files.createDirectory(dir);
        for (int i = 1; i <= 25; i++) {
            Files.writeString(dir.resolve("f" + i + ".txt"), "x");
        }

        ToolRequest request = ToolRequest.builder()
                .prompt("List bulk")
                .build();

        ToolResponse response = tool.execute(request);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("TRUNCATED"));
        assertTrue(response.getMessage().contains("first 10 items"));
        @SuppressWarnings("unchecked")
        List<String> data = (List<String>) response.getData();
        assertEquals(10, data.size());
    }

    @Test
    void testGlobPatternNonTruncated() throws IOException {
        Path dir = tempDir.resolve("g");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("one.md"), "1");
        Files.writeString(dir.resolve("two.md"), "2");

        ToolRequest request = ToolRequest.builder()
                .prompt("List g/*.md")
                .build();

        ToolResponse response = tool.execute(request);
        assertTrue(response.isSuccess());
        assertFalse(response.getMessage().contains("TRUNCATED"));
        @SuppressWarnings("unchecked")
        List<String> data = (List<String>) response.getData();
        assertEquals(2, data.size());
    }

    @Test
    void testGlobPatternTruncated() throws IOException {
        Path dir = tempDir.resolve("many");
        Files.createDirectory(dir);
        IntStream.rangeClosed(1, 30).forEach(i -> {
            try {
                Files.writeString(dir.resolve("file" + i + ".java"), "class X{}\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        ToolRequest request = ToolRequest.builder()
                .prompt("List many/*.java")
                .build();

        ToolResponse response = tool.execute(request);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("TRUNCATED"));
        @SuppressWarnings("unchecked")
        List<String> data = (List<String>) response.getData();
        assertEquals(10, data.size());
    }

    @Test
    void testListDirectoryNotFound() {
        ToolRequest request = ToolRequest.builder()
                .prompt("List missing")
                .build();
        ToolResponse response = tool.execute(request);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Directory not found"));
    }

    @Test
    void testListSecurityViolation() {
        ToolRequest request = ToolRequest.builder()
                .prompt("List ../../etc")
                .build();
        ToolResponse response = tool.execute(request);
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Security error"));
    }
}
