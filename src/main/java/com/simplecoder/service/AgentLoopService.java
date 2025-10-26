package com.simplecoder.service;

import com.simplecoder.config.AgentLoopProperties;
import com.simplecoder.exception.RecoverableException;
import com.simplecoder.exception.TerminalException;
import com.simplecoder.model.AggregatedResultFormatter;
import com.simplecoder.model.ExecutionStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal loop skeleton (R-6): executes a fixed number of steps determined by configuration.
 * R-8: Integrates exception handling - RecoverableException continues loop, TerminalException aborts.
 * IF-2: Adds early termination via repetition detection to prevent wasteful iterations.
 */
public class AgentLoopService {

    //    todo: too many magic number and long methods in this file. need to make it more srp and more solid, without breaking behavior

    private final AgentLoopProperties properties;
    private final SingleTurnExecutor executor;

    public AgentLoopService(AgentLoopProperties properties, SingleTurnExecutor executor) {
        this.properties = properties;
        this.executor = executor;
    }

    /**
     * Run the loop for the configured maximum number of steps.
     * R-8: Catches RecoverableException (continues) and TerminalException (aborts).
     * IF-2: Early stops on TERMINATION_SIGNAL:COMPLETED or repeated normalized summaries.
     * IF-3: Accumulates context from previous steps (last 3) to enable informed decision-making.
     *
     * @param initialPrompt initial user prompt
     * @return LoopResult containing individual steps and aggregated formatting
     */
    public LoopResult runLoop(String initialPrompt) {
        int max = properties.getMaxSteps();
        List<ExecutionStep> steps = new ArrayList<>(max);
        boolean terminated = false;
        String reason = null;
        String previousNormalizedSummary = null;

        for (int i = 1; i <= max; i++) { // 1-based step numbers per tests
            // Build prompt with context from recent steps (last 3)
            String currentPrompt = buildPromptWithContext(initialPrompt, steps);

            TurnResult result;
            String toolName = null;
            String raw;
            try {
                result = executor.execute(currentPrompt);
                raw = result.content();
                toolName = result.toolName();
            } catch (RecoverableException e) {
                // R-8: Recoverable exception - capture as observation, continue loop
                raw = e.getMessage();
            } catch (TerminalException e) {
                // R-8: Terminal exception - capture error, abort loop immediately
                raw = e.getTerminationReason() + ": " + e.getMessage();
                String summary = truncate(raw, 80);
                ExecutionStep step = new ExecutionStep(
                        i,
                        initialPrompt,
                        null, // no tool name for exception case
                        summary,
                        "" // tasks snapshot deferred
                );
                steps.add(step);
                terminated = true;
                reason = e.getTerminationReason();
                break;
            }

            String summary = truncate(raw, 80);
            ExecutionStep step = new ExecutionStep(
                    i,
                    initialPrompt,
                    toolName, // IF-4: real tool name from execution result
                    summary,
                    "" // tasks snapshot deferred
            );
            steps.add(step);

            // R-7 prewritten tests: detect completion signal
            if (summary.contains("TERMINATION_SIGNAL:COMPLETED")) {
                terminated = true;
                reason = "COMPLETED";
                break;
            }

            // IF-2: Early stop on repetition (consecutive identical normalized summaries)
            String normalizedSummary = normalizeSummary(summary);
            if (normalizedSummary.equals(previousNormalizedSummary)) {
                terminated = true;
                reason = "COMPLETED"; // treat repetition as implicit completion
                break;
            }
            previousNormalizedSummary = normalizedSummary;
        }
        if (!terminated) { // if loop exhausted steps
            terminated = true;
            reason = "STEP_LIMIT";
        }
        String aggregated = AggregatedResultFormatter.format(steps, terminated, reason);
        com.simplecoder.model.ExecutionContext finalCtx = new com.simplecoder.model.ExecutionContext(
                steps.size(),
                max,
                terminated,
                reason
        );
        return new LoopResult(List.copyOf(steps), aggregated, finalCtx);
    }

    /**
     * Build prompt with context from recent steps (last 3).
     * Format:
     * <pre>
     * [Previous Step Summaries]
     * Step 1: summary1
     * Step 2: summary2
     * ...
     *
     * [User Request]
     * original prompt
     * </pre>
     *
     * @param initialPrompt original user request
     * @param steps         list of previous execution steps
     * @return prompt with context appended
     */
    private String buildPromptWithContext(String initialPrompt, List<ExecutionStep> steps) {
        if (steps.isEmpty()) {
            return initialPrompt;
        }

        StringBuilder context = new StringBuilder();
        context.append("[Previous Step Summaries]\n");

        // Include last 3 steps (or fewer if less than 3 steps executed)
        int startIdx = Math.max(0, steps.size() - 3);
        for (int i = startIdx; i < steps.size(); i++) {
            ExecutionStep step = steps.get(i);
            context.append("Step ").append(step.stepNumber()).append(": ")
                    .append(step.resultSummary()).append("\n");
        }

        context.append("\n[User Request]\n").append(initialPrompt);
        return context.toString();
    }

    /**
     * Normalize a summary for repetition detection.
     * Removes punctuation, converts to lowercase, and trims whitespace.
     *
     * @param summary raw summary string
     * @return normalized string
     */
    private String normalizeSummary(String summary) {
        if (summary == null) return "";
        // Remove all non-alphanumeric characters and whitespace, convert to lowercase
        return summary.replaceAll("[^a-zA-Z0-9\\s]", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
