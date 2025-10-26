package com.simplecoder.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExecutionStepTest {

    @Test
    void testBasicConstruction() {
        ExecutionStep step = new ExecutionStep(
                1,
                "Read file src/main/java/Main.java",
                "readFile",
                "File content retrieved successfully",
                "[{id:1, status:'completed'}]"
        );

        assertEquals(1, step.stepNumber());
        assertEquals("Read file src/main/java/Main.java", step.actionPrompt());
        assertEquals("readFile", step.toolName());
        assertEquals("File content retrieved successfully", step.resultSummary());
        assertEquals("[{id:1, status:'completed'}]", step.tasksSnapshot());
    }

    @Test
    void testConstructionWithNullTasksSnapshot() {
        ExecutionStep step = new ExecutionStep(
                2,
                "Search for pattern",
                "searchText",
                "Found 5 matches",
                null
        );

        assertEquals(2, step.stepNumber());
        assertEquals("Search for pattern", step.actionPrompt());
        assertEquals("searchText", step.toolName());
        assertEquals("Found 5 matches", step.resultSummary());
        assertNull(step.tasksSnapshot());
    }

    @Test
    void testConstructionWithEmptyStrings() {
        ExecutionStep step = new ExecutionStep(
                0,
                "",
                "",
                "",
                ""
        );

        assertEquals(0, step.stepNumber());
        assertEquals("", step.actionPrompt());
        assertEquals("", step.toolName());
        assertEquals("", step.resultSummary());
        assertEquals("", step.tasksSnapshot());
    }

    @Test
    void testConstructionWithNegativeStepNumber() {
        ExecutionStep step = new ExecutionStep(
                -1,
                "Invalid step",
                "unknownTool",
                "Error occurred",
                null
        );

        assertEquals(-1, step.stepNumber());
        assertEquals("Invalid step", step.actionPrompt());
        assertEquals("unknownTool", step.toolName());
        assertEquals("Error occurred", step.resultSummary());
        assertNull(step.tasksSnapshot());
    }

    @Test
    void testEqualsAndHashCode() {
        ExecutionStep step1 = new ExecutionStep(
                1,
                "Action",
                "tool",
                "Result",
                "Tasks"
        );
        ExecutionStep step2 = new ExecutionStep(
                1,
                "Action",
                "tool",
                "Result",
                "Tasks"
        );
        ExecutionStep step3 = new ExecutionStep(
                2,
                "Action",
                "tool",
                "Result",
                "Tasks"
        );

        assertEquals(step1, step2);
        assertNotEquals(step1, step3);
        assertEquals(step1.hashCode(), step2.hashCode());
        assertNotEquals(step1.hashCode(), step3.hashCode());
    }

    @Test
    void testToString() {
        ExecutionStep step = new ExecutionStep(
                1,
                "Read file",
                "readFile",
                "Success",
                null
        );

        String toString = step.toString();
        assertTrue(toString.contains("ExecutionStep"));
        assertTrue(toString.contains("stepNumber=1"));
        assertTrue(toString.contains("actionPrompt=Read file"));
        assertTrue(toString.contains("toolName=readFile"));
        assertTrue(toString.contains("resultSummary=Success"));
    }

    @Test
    void testImmutability() {
        ExecutionStep step = new ExecutionStep(
                1,
                "Original action",
                "originalTool",
                "Original result",
                "Original tasks"
        );

        // Records are immutable - accessors return copies, cannot modify
        assertEquals(1, step.stepNumber());
        assertEquals("Original action", step.actionPrompt());

        // Verify all fields remain unchanged
        assertEquals("originalTool", step.toolName());
        assertEquals("Original result", step.resultSummary());
        assertEquals("Original tasks", step.tasksSnapshot());
    }

    @Test
    void testMultipleStepsScenario() {
        // Simulate a multi-step execution sequence
        ExecutionStep step1 = new ExecutionStep(
                1,
                "List all Java files",
                "listFiles",
                "Found 15 files",
                "[{id:1, content:'List files', status:'in_progress'}]"
        );

        ExecutionStep step2 = new ExecutionStep(
                2,
                "Read Main.java",
                "readFile",
                "Read 100 lines",
                "[{id:1, content:'List files', status:'completed'}, {id:2, content:'Read files', status:'in_progress'}]"
        );

        ExecutionStep step3 = new ExecutionStep(
                3,
                "Search for 'TODO'",
                "searchText",
                "Found 3 occurrences",
                "[{id:1, status:'completed'}, {id:2, status:'completed'}, {id:3, status:'completed'}]"
        );

        assertEquals(1, step1.stepNumber());
        assertEquals(2, step2.stepNumber());
        assertEquals(3, step3.stepNumber());

        assertNotEquals(step1.tasksSnapshot(), step2.tasksSnapshot());
        assertNotEquals(step2.tasksSnapshot(), step3.tasksSnapshot());
    }

    @Test
    void testNullActionPrompt() {
        ExecutionStep step = new ExecutionStep(
                1,
                null,
                "tool",
                "result",
                "tasks"
        );

        assertNull(step.actionPrompt());
    }

    @Test
    void testNullToolName() {
        ExecutionStep step = new ExecutionStep(
                1,
                "action",
                null,
                "result",
                "tasks"
        );

        assertNull(step.toolName());
    }

    @Test
    void testNullResultSummary() {
        ExecutionStep step = new ExecutionStep(
                1,
                "action",
                "tool",
                null,
                "tasks"
        );

        assertNull(step.resultSummary());
    }

    @Test
    void testLargeStepNumber() {
        ExecutionStep step = new ExecutionStep(
                Integer.MAX_VALUE,
                "Final step",
                "completionTool",
                "All tasks completed",
                null
        );

        assertEquals(Integer.MAX_VALUE, step.stepNumber());
    }

    @Test
    void testLongStringsInFields() {
        String longAction = "A".repeat(1000);
        String longResult = "B".repeat(1000);
        String longTasks = "C".repeat(1000);

        ExecutionStep step = new ExecutionStep(
                1,
                longAction,
                "tool",
                longResult,
                longTasks
        );

        assertEquals(longAction, step.actionPrompt());
        assertEquals(longResult, step.resultSummary());
        assertEquals(longTasks, step.tasksSnapshot());
        assertEquals(1000, step.actionPrompt().length());
    }
}
