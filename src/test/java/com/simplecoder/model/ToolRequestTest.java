package com.simplecoder.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolRequestTest {

    @Test
    void testBuilderWithDefaults() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Read file.txt")
                .build();

        assertEquals("Read file.txt", request.getPrompt());
        assertEquals("auto", request.getToolType()); // Default value
        assertNull(request.getContextHistory());
    }

    @Test
    void testBuilderWithAllFields() {
        List<ContextEntry> history = List.of(
                ContextEntry.builder().prompt("Previous context 1").result("Result 1").build(),
                ContextEntry.builder().prompt("Previous context 2").result("Result 2").build()
        );
        ToolRequest request = ToolRequest.builder()
                .prompt("Search for Agent")
                .toolType("search")
                .contextHistory(history)
                .build();

        assertEquals("Search for Agent", request.getPrompt());
        assertEquals("search", request.getToolType());
        assertEquals(history, request.getContextHistory());
    }

    @Test
    void testValidateSuccess() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Valid prompt")
                .toolType("read")
                .build();

        assertDoesNotThrow(request::validate);
    }

    @Test
    void testValidateFailsWithNullPrompt() {
        ToolRequest request = ToolRequest.builder()
                .prompt(null)
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                request::validate
        );
        assertEquals("Prompt cannot be null or empty", exception.getMessage());
    }

    @Test
    void testValidateFailsWithEmptyPrompt() {
        ToolRequest request = ToolRequest.builder()
                .prompt("   ")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                request::validate
        );
        assertEquals("Prompt cannot be null or empty", exception.getMessage());
    }

    @Test
    void testValidateFailsWithNullToolType() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Valid prompt")
                .toolType(null)
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                request::validate
        );
        assertEquals("ToolType cannot be null or empty", exception.getMessage());
    }

    @Test
    void testValidateFailsWithEmptyToolType() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Valid prompt")
                .toolType("  ")
                .build();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                request::validate
        );
        assertEquals("ToolType cannot be null or empty", exception.getMessage());
    }

    @Test
    void testNoArgsConstructor() {
        ToolRequest request = new ToolRequest();
        assertNull(request.getPrompt());
        assertEquals("auto", request.getToolType()); // @Builder.Default applies
        assertNull(request.getContextHistory());
    }

    @Test
    void testAllArgsConstructor() {
        List<ContextEntry> history = List.of(
                ContextEntry.builder().prompt("Context 1").result("Result 1").build()
        );
        ToolRequest request = new ToolRequest("Test prompt", "list", history);

        assertEquals("Test prompt", request.getPrompt());
        assertEquals("list", request.getToolType());
        assertEquals(history, request.getContextHistory());
    }

    @Test
    void testSettersAndGetters() {
        ToolRequest request = new ToolRequest();
        List<ContextEntry> history = List.of(
                ContextEntry.builder().prompt("Context").result("Result").build()
        );

        request.setPrompt("New prompt");
        request.setToolType("replace");
        request.setContextHistory(history);

        assertEquals("New prompt", request.getPrompt());
        assertEquals("replace", request.getToolType());
        assertEquals(history, request.getContextHistory());
    }

    @Test
    void testBuildContextSummaryWithHistory() {
        List<ContextEntry> history = List.of(
                ContextEntry.builder().prompt("Prompt 1").result("Result 1").build(),
                ContextEntry.builder().prompt("Prompt 2").result("Result 2").build()
        );
        ToolRequest request = ToolRequest.builder()
                .prompt("Test")
                .contextHistory(history)
                .build();

        String summary = request.buildContextSummary();

        assertTrue(summary.contains("--- Recent Context ---"));
        assertTrue(summary.contains("User: Prompt 1"));
        assertTrue(summary.contains("Result: Result 1"));
        assertTrue(summary.contains("User: Prompt 2"));
        assertTrue(summary.contains("Result: Result 2"));
        assertTrue(summary.contains("--- End Context ---"));
    }

    @Test
    void testBuildContextSummaryWithEmptyHistory() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Test")
                .contextHistory(List.of())
                .build();

        String summary = request.buildContextSummary();

        assertEquals("No previous context.", summary);
    }

    @Test
    void testBuildContextSummaryWithNullHistory() {
        ToolRequest request = ToolRequest.builder()
                .prompt("Test")
                .contextHistory(null)
                .build();

        String summary = request.buildContextSummary();

        assertEquals("No previous context.", summary);
    }
}
