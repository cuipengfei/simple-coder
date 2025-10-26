package com.simplecoder.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R-9 tests for aggregation output format specification.
 * These tests are written BEFORE implementing AggregatedResultFormatter (TDD).
 */
class AggregatedResultFormatterTest {

    @Test
    @DisplayName("Aggregated output contains all step indices and correct task counts per step plus total line")
    void aggregatesIndicesAndTaskCounts() {
        List<ExecutionStep> steps = List.of(
                new ExecutionStep(1, "Action 1", "readFile", "Short result", "t1:PENDING,t2:IN_PROGRESS,t3:COMPLETED"),
                new ExecutionStep(2, "Action 2", "searchText", "Another result", "t1:COMPLETED,t2:IN_PROGRESS,t3:COMPLETED,t4:PENDING"),
                new ExecutionStep(3, "Action 3", "replaceText", "Final result", "t1:COMPLETED,t2:COMPLETED,t3:COMPLETED")
        );

        String aggregated = AggregatedResultFormatter.format(steps, true, "STEP_LIMIT");
        String[] lines = aggregated.split("\r?\n");

        // Header + 3 step lines + total line
        assertThat(lines.length).isEqualTo(5);
        assertThat(lines[0]).startsWith("HEADER:");

        assertThat(lines[1]).contains("STEP 1 | tool=readFile").contains("tasks total=3 pending=1 inProgress=1 completed=1");
        assertThat(lines[2]).contains("STEP 2 | tool=searchText").contains("tasks total=4 pending=1 inProgress=1 completed=2");
        assertThat(lines[3]).contains("STEP 3 | tool=replaceText").contains("tasks total=3 pending=0 inProgress=0 completed=3");

        assertThat(lines[4])
                .startsWith("TOTAL_STEPS=3")
                .contains("TERMINATED=true")
                .contains("REASON=STEP_LIMIT");
    }

    @Test
    @DisplayName("Summary longer than 200 chars is truncated with ellipsis and internal quotes kept")
    void summaryTruncationAndQuotesPreserved() {
        String longSummary = "\"" + "A".repeat(205) + "\" end"; // leading and trailing quotes inside
        List<ExecutionStep> steps = List.of(
                new ExecutionStep(1, "Action", "toolX", longSummary, "t1:PENDING")
        );
        String aggregated = AggregatedResultFormatter.format(steps, false, null);
        String[] lines = aggregated.split("\r?\n");
        assertThat(lines.length).isEqualTo(3); // header + step + total

        String stepLine = lines[1];
        // Ensure truncation marker present
        assertThat(stepLine).contains("…");
        // Extract summary segment between summary=" and " | tasks
        int start = stepLine.indexOf("summary=\"") + 9;
        int after = stepLine.indexOf("\" | tasks");
        String inner = stepLine.substring(start, after);
        // length should be 201 (200 chars + ellipsis) OR less if edge, but not > 201
        assertThat(inner.length()).isLessThanOrEqualTo(201);
        assertThat(inner.endsWith("…"));
        // Internal original double quotes at start should remain (the first character is ")
        assertThat(inner.charAt(0)).isEqualTo('"');
    }

    @Test
    @DisplayName("Empty or unparsable tasksSnapshot counted as zeros")
    void emptyTasksSnapshotHandled() {
        List<ExecutionStep> steps = List.of(
                new ExecutionStep(1, "Action", "toolX", "Result", ""),
                new ExecutionStep(2, "Action2", "toolY", "Result2", "nonsense-format-that-will-not-parse")
        );
        String aggregated = AggregatedResultFormatter.format(steps, false, null);
        String[] lines = aggregated.split("\r?\n");
        assertThat(lines[1]).contains("tasks total=0 pending=0 inProgress=0 completed=0");
        assertThat(lines[2]).contains("tasks total=0 pending=0 inProgress=0 completed=0");
    }
}
