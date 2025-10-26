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
     * IF-2: Early stops on TERMINATION_SIGNAL:COMPLETED or repeated display summaries (post-truncation) to avoid suffix-only differences.
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
        String previousNormalizedDisplay = null; // stores normalized truncated summary from previous step

        for (int i = 1; i <= max; i++) { // 1-based step numbers per tests
            String currentPrompt = buildPromptWithContext(initialPrompt, steps);

            TurnResult result;
            String toolName = null;
            String raw;
            try {
                result = executor.execute(currentPrompt);
                raw = result.content();
                toolName = result.toolName();
            } catch (RecoverableException e) {
                raw = e.getMessage();
            } catch (TerminalException e) {
                raw = e.getTerminationReason() + ": " + e.getMessage();
                String summary = truncate(raw, 80);
                ExecutionStep step = new ExecutionStep(
                        i,
                        initialPrompt,
                        null,
                        summary,
                        ""
                );
                steps.add(step);
                terminated = true;
                reason = e.getTerminationReason();
                break;
            }

            // Normalize raw for explicit termination signal detection only
            String normalizedRaw = normalizeSummary(raw);

            if (normalizedRaw.contains("terminationsignalcompleted")) {
                String summary = truncate(raw, 80);
                ExecutionStep step = new ExecutionStep(
                        i,
                        initialPrompt,
                        toolName,
                        summary,
                        ""
                );
                steps.add(step);
                terminated = true;
                reason = "COMPLETED";
                break;
            }

            // Display-based repetition detection: operate on truncated summary user actually sees
            String summary = truncate(raw, 80);
            String normalizedDisplay = normalizeSummary(summary);
            if (previousNormalizedDisplay != null && normalizedDisplay.equals(previousNormalizedDisplay)) {
                ExecutionStep step = new ExecutionStep(
                        i,
                        initialPrompt,
                        toolName,
                        summary,
                        ""
                );
                steps.add(step);
                terminated = true;
                reason = "COMPLETED"; // implicit completion due to repeated display output
                break;
            }
            previousNormalizedDisplay = normalizedDisplay;

            ExecutionStep step = new ExecutionStep(
                    i,
                    initialPrompt,
                    toolName,
                    summary,
                    ""
            );
            steps.add(step);
        }
        if (!terminated) {
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

    private String buildPromptWithContext(String initialPrompt, List<ExecutionStep> steps) {
        if (steps.isEmpty()) {
            return initialPrompt;
        }
        StringBuilder context = new StringBuilder();
        context.append("[Previous Step Summaries]\n");
        int startIdx = Math.max(0, steps.size() - 3);
        for (int i = startIdx; i < steps.size(); i++) {
            ExecutionStep step = steps.get(i);
            context.append("Step ").append(step.stepNumber()).append(": ")
                    .append(step.resultSummary()).append("\n");
        }
        context.append("\n[User Request]\n").append(initialPrompt);
        return context.toString();
    }

    private String normalizeSummary(String summary) {
        if (summary == null) return "";
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
