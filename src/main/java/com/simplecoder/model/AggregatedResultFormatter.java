package com.simplecoder.model;

import java.util.List;
import java.util.Locale;

/**
 * Formats a list of ExecutionStep objects into a concise multi-line text representation.
 * Roadmap R-9 scope: simple immutable formatting (no streaming, no external deps).
 */
public final class AggregatedResultFormatter {

//    todo: too many magic number and long methods in this file. need to make it more srp and more solid, without breaking behavior

    private static final int MAX_SUMMARY_CHARS = 200; // per specification
    private static final char ELLIPSIS = '…';

    private AggregatedResultFormatter() {
    }

    /**
     * Format steps into header + one line per step + total line.
     *
     * @param steps      list of steps (may be empty)
     * @param terminated termination flag
     * @param reason     termination reason (nullable)
     * @return aggregated multiline string
     */
    public static String format(List<ExecutionStep> steps, boolean terminated, String reason) {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb);
        for (ExecutionStep step : steps) {
            sb.append(System.lineSeparator())
                    .append(formatStepLine(step));
        }
        sb.append(System.lineSeparator())
                .append(formatTotalLine(steps.size(), terminated, reason));
        return sb.toString();
    }

    private static void appendHeader(StringBuilder sb) {
        sb.append("HEADER: STEP n | tool=<tool> | summary=\"...\" | tasks total=T pending=P inProgress=IP completed=C");
    }

    private static String formatStepLine(ExecutionStep step) {
        String tool = safe(step.toolName());
        if (tool.isEmpty()) tool = "-";
        String summary = normalizeSummary(step.resultSummary());
        summary = truncateIfNeeded(summary);
        TaskCounts counts = parseTaskSnapshot(step.tasksSnapshot());
        return "STEP " + step.stepNumber() +
                " | tool=" + tool +
                " | summary=\"" + summary + "\"" +
                " | tasks total=" + counts.total +
                " pending=" + counts.pending +
                " inProgress=" + counts.inProgress +
                " completed=" + counts.completed;
    }

    private static String formatTotalLine(int totalSteps, boolean terminated, String reason) {
        String r = (reason == null || reason.isBlank()) ? "none" : reason;
        return "TOTAL_STEPS=" + totalSteps + " TERMINATED=" + terminated + " REASON=" + r;
    }

    private static String normalizeSummary(String raw) {
        if (raw == null || raw.isBlank()) return "-";
        // collapse newlines to single space
        String s = raw.replace('\n', ' ').replace('\r', ' ');
        // collapse multiple spaces
        return s.trim().replaceAll("\\s+", " ");
    }

    private static String truncateIfNeeded(String s) {
        if (s.length() <= MAX_SUMMARY_CHARS) return s;
        return s.substring(0, MAX_SUMMARY_CHARS) + ELLIPSIS;
    }

    private record TaskCounts(int total, int pending, int inProgress, int completed) {
    }

    private static TaskCounts parseTaskSnapshot(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return new TaskCounts(0, 0, 0, 0);
        }
        String[] parts = snapshot.split(",");
        int total = 0, p = 0, ip = 0, c = 0;
        for (String part : parts) {
            String trimmed = part.trim();
            int idx = trimmed.indexOf(':');
            if (idx <= 0 || idx == trimmed.length() - 1) {
                continue; // malformed
            }
            String status = trimmed.substring(idx + 1).trim().toUpperCase(Locale.ROOT);
            boolean recognized = false;
            switch (status) {
                case "PENDING" -> {
                    p++;
                    recognized = true;
                }
                case "IN_PROGRESS" -> {
                    ip++;
                    recognized = true;
                }
                case "COMPLETED" -> {
                    c++;
                    recognized = true;
                }
                default -> { /* ignore unknown */ }
            }
            if (recognized) total++;
        }
        return new TaskCounts(total, p, ip, c);
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
