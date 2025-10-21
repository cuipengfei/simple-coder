package com.simplecoder.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContextEntry, focusing on the no-truncation behavior after refactoring.
 */
class ContextEntryTest {

    @Test
    void testGetSummary_shortContent() {
        ContextEntry entry = ContextEntry.builder()
                .prompt("Short prompt")
                .result("Short result")
                .build();

        String summary = entry.getSummary();

        assertTrue(summary.contains("Short prompt"));
        assertTrue(summary.contains("Short result"));
    }

    @Test
    void testGetSummary_longPrompt_noTruncation() {
        String longPrompt = "a".repeat(1000);
        ContextEntry entry = ContextEntry.builder()
                .prompt(longPrompt)
                .result("result")
                .build();

        String summary = entry.getSummary();

        // Verify full prompt is included (no truncation)
        assertTrue(summary.contains(longPrompt), "Summary should contain full 1000-char prompt");
        assertEquals(1000, countOccurrences(summary, "a"), "All 1000 'a' characters should be present");
    }

    @Test
    void testGetSummary_longResult_noTruncation() {
        String longResult = "b".repeat(2000);
        ContextEntry entry = ContextEntry.builder()
                .prompt("prompt")
                .result(longResult)
                .build();

        String summary = entry.getSummary();

        // Verify full result is included (no truncation)
        assertTrue(summary.contains(longResult), "Summary should contain full 2000-char result");
        assertEquals(2000, countOccurrences(summary, "b"), "All 2000 'b' characters should be present");
    }

    @Test
    void testGetSummary_bothLong_noTruncation() {
        String longPrompt = "x".repeat(1500);
        String longResult = "y".repeat(1500);
        ContextEntry entry = ContextEntry.builder()
                .prompt(longPrompt)
                .result(longResult)
                .build();

        String summary = entry.getSummary();

        // Verify both are fully included
        assertTrue(summary.contains(longPrompt));
        assertTrue(summary.contains(longResult));
        assertEquals(1500, countOccurrences(summary, "x"));
        assertEquals(1500, countOccurrences(summary, "y"));
    }

    @Test
    void testGetSummary_nullPrompt() {
        ContextEntry entry = ContextEntry.builder()
                .prompt(null)
                .result("result")
                .build();

        String summary = entry.getSummary();

        assertTrue(summary.contains("null"), "Should handle null prompt gracefully");
        assertTrue(summary.contains("result"));
    }

    @Test
    void testGetSummary_nullResult() {
        ContextEntry entry = ContextEntry.builder()
                .prompt("prompt")
                .result(null)
                .build();

        String summary = entry.getSummary();

        assertTrue(summary.contains("prompt"));
        assertTrue(summary.contains("null"), "Should handle null result gracefully");
    }

    @Test
    void testGetSummary_bothNull() {
        ContextEntry entry = ContextEntry.builder()
                .prompt(null)
                .result(null)
                .build();

        String summary = entry.getSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("null"));
    }

    @Test
    void testGetSummary_containsTimestamp() {
        ContextEntry entry = ContextEntry.builder()
                .prompt("test")
                .result("test")
                .build();

        String summary = entry.getSummary();

        // Summary should contain timestamp in format [timestamp]
        assertTrue(summary.matches(".*\\[\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.*\\].*"));
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if (String.valueOf(c).equals(target)) {
                count++;
            }
        }
        return count;
    }
}
