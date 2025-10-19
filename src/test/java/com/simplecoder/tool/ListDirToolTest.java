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

class ListDirToolTest {

    @TempDir
    Path tempDir;

    private ListDirTool tool;
    private PathValidator pathValidator;

    @BeforeEach
    void setUp() {
        pathValidator = new PathValidator(tempDir.toString());
        tool = new ListDirTool(pathValidator);
    }

    @Test
    void testGetName() {
        assertEquals("list", tool.getName());
    }

    @Test
    void testListEmptyDirectory() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectory(emptyDir);

        ToolRequest request = ToolRequest.builder()
                .prompt("List empty")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Found 0 items"));

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertTrue(results.isEmpty());
    }

    @Test
    void testListDirectoryWithFiles() throws IOException {
        Path dir = tempDir.resolve("testdir");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("file1.txt"), "content1");
        Files.writeString(dir.resolve("file2.txt"), "content2");
        Files.createDirectory(dir.resolve("subdir"));

        ToolRequest request = ToolRequest.builder()
                .prompt("List testdir")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Found 3 items"));

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(3, results.size());
        assertTrue(results.contains("testdir/file1.txt"));
        assertTrue(results.contains("testdir/file2.txt"));
        assertTrue(results.contains("testdir/subdir"));
    }

    @Test
    void testListNonExistentDirectory() {
        ToolRequest request = ToolRequest.builder()
                .prompt("List nonexistent")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Directory not found"));
    }

    @Test
    void testListFile() throws IOException {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "content");

        ToolRequest request = ToolRequest.builder()
                .prompt("List file.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("not a directory"));
    }

    @Test
    void testListRootDirectory() throws IOException {
        Files.writeString(tempDir.resolve("root1.txt"), "content");
        Files.writeString(tempDir.resolve("root2.txt"), "content");

        ToolRequest request = ToolRequest.builder()
                .prompt("List .")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertTrue(results.contains("root1.txt"));
        assertTrue(results.contains("root2.txt"));
    }

    @Test
    void testListWithGlobPattern() throws IOException {
        // Create structure:
        // src/main/File1.java
        // src/main/File2.java
        // src/test/Test1.java
        Path srcMain = tempDir.resolve("src/main");
        Path srcTest = tempDir.resolve("src/test");
        Files.createDirectories(srcMain);
        Files.createDirectories(srcTest);
        Files.writeString(srcMain.resolve("File1.java"), "");
        Files.writeString(srcMain.resolve("File2.java"), "");
        Files.writeString(srcTest.resolve("Test1.java"), "");
        Files.writeString(srcMain.resolve("readme.txt"), "");

        ToolRequest request = ToolRequest.builder()
                .prompt("List src/**/*.java")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Found 3 items"));

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(3, results.size());
        assertTrue(results.contains("src/main/File1.java"));
        assertTrue(results.contains("src/main/File2.java"));
        assertTrue(results.contains("src/test/Test1.java"));
        assertFalse(results.contains("src/main/readme.txt"));
    }

    @Test
    void testListWithWildcardPattern() throws IOException {
        Path dir = tempDir.resolve("files");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("test1.txt"), "");
        Files.writeString(dir.resolve("test2.txt"), "");
        Files.writeString(dir.resolve("data.csv"), "");

        ToolRequest request = ToolRequest.builder()
                .prompt("List files/*.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(2, results.size());
        assertTrue(results.contains("files/test1.txt"));
        assertTrue(results.contains("files/test2.txt"));
        assertFalse(results.contains("files/data.csv"));
    }

    @Test
    void testListNestedDirectory() throws IOException {
        Path nested = tempDir.resolve("a/b/c");
        Files.createDirectories(nested);
        Files.writeString(nested.resolve("deep.txt"), "content");

        ToolRequest request = ToolRequest.builder()
                .prompt("List a/b/c")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(1, results.size());
        assertTrue(results.contains("a/b/c/deep.txt"));
    }

    @Test
    void testListPathOutsideRepo() {
        ToolRequest request = ToolRequest.builder()
                .prompt("List ../../etc")
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Security error"));
    }

    @Test
    void testListWithQuestionMarkGlob() throws IOException {
        Path dir = tempDir.resolve("docs");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("file1.txt"), "");
        Files.writeString(dir.resolve("file2.txt"), "");
        Files.writeString(dir.resolve("file10.txt"), "");

        ToolRequest request = ToolRequest.builder()
                .prompt("List docs/file?.txt")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(2, results.size());
        assertTrue(results.contains("docs/file1.txt"));
        assertTrue(results.contains("docs/file2.txt"));
        assertFalse(results.contains("docs/file10.txt"));
    }

    @Test
    void testListPromptWithCaseInsensitivePrefix() throws IOException {
        Path dir = tempDir.resolve("test");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("file.txt"), "");

        ToolRequest request = ToolRequest.builder()
                .prompt("LIST test")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
    }

    @Test
    void testListNullPrompt() {
        ToolRequest request = ToolRequest.builder()
                .prompt(null)
                .build();

        ToolResponse response = tool.execute(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("Invalid request"));
    }

    @Test
    void testListResultsAreSorted() throws IOException {
        Path dir = tempDir.resolve("sorted");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("zebra.txt"), "");
        Files.writeString(dir.resolve("apple.txt"), "");
        Files.writeString(dir.resolve("banana.txt"), "");

        ToolRequest request = ToolRequest.builder()
                .prompt("List sorted")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(3, results.size());
        assertEquals("sorted/apple.txt", results.get(0));
        assertEquals("sorted/banana.txt", results.get(1));
        assertEquals("sorted/zebra.txt", results.get(2));
    }

    @Test
    void testListGlobMatchingDirectories() throws IOException {
        Files.createDirectories(tempDir.resolve("src/main"));
        Files.createDirectories(tempDir.resolve("src/test"));
        Files.createDirectories(tempDir.resolve("docs"));

        ToolRequest request = ToolRequest.builder()
                .prompt("List src/*")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());

        @SuppressWarnings("unchecked")
        List<String> results = (List<String>) response.getData();
        assertEquals(2, results.size());
        assertTrue(results.contains("src/main"));
        assertTrue(results.contains("src/test"));
        assertFalse(results.contains("docs"));
    }

    @Test
    void testListWithWhitespace() throws IOException {
        Path dir = tempDir.resolve("test");
        Files.createDirectory(dir);
        Files.writeString(dir.resolve("file.txt"), "");

        ToolRequest request = ToolRequest.builder()
                .prompt("  List  test  ")
                .build();

        ToolResponse response = tool.execute(request);

        assertTrue(response.isSuccess());
    }
}
