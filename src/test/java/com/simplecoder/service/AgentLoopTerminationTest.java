package com.simplecoder.service;

import com.simplecoder.config.AgentLoopProperties;
import com.simplecoder.model.ExecutionContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R-7 termination checks tests (written before implementation).
 *
 * Tests two termination conditions:
 * 1. STEP_LIMIT: maxSteps reached (loop must stop)
 * 2. COMPLETED: placeholder early completion signal (future TODO system integration)
 *
 * Verification points per ROADMAP R-7:
 * - Loop stops at maxSteps
 * - Last termination reason == STEP_LIMIT when limit reached
 * - COMPLETED termination path exists (placeholder, no real detection yet)
 */
class AgentLoopTerminationTest {

    @Test
    @DisplayName("Loop stops when stepCount reaches maxSteps and sets reason to STEP_LIMIT")
    void loopStopsAtMaxStepsWithCorrectReason() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(3);

        // Stub executor that never signals completion
        int[] callCount = {0};
        SingleTurnExecutor executor = (prompt) -> TurnResult.withoutTool("step result " + (++callCount[0]));

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("user request");

        // Verify loop executed exactly maxSteps
        assertThat(result.steps()).hasSize(3);

        // Verify aggregated output contains termination reason
        String aggregated = result.aggregated();
        assertThat(aggregated).contains("TERMINATED=true");
        assertThat(aggregated).contains("REASON=STEP_LIMIT");
        assertThat(aggregated).contains("TOTAL_STEPS=3");
    }

    @Test
    @DisplayName("Loop stops before maxSteps when COMPLETED termination is signaled (placeholder)")
    void loopStopsEarlyOnCompletedSignal() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(10);

        // Stub executor that signals completion after 2 steps
        int[] callCount = {0};
        SingleTurnExecutor executor = (prompt) -> {
            callCount[0]++;
            if (callCount[0] == 2) {
                return TurnResult.withoutTool("TERMINATION_SIGNAL:COMPLETED"); // Placeholder completion signal
            }
            return TurnResult.withoutTool("step result");
        };

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("user request");

        // Verify loop stopped early (before maxSteps=10)
        assertThat(result.steps()).hasSize(2);

        // Verify aggregated output shows COMPLETED termination
        String aggregated = result.aggregated();
        assertThat(aggregated).contains("TERMINATED=true");
        assertThat(aggregated).contains("REASON=COMPLETED");
        assertThat(aggregated).contains("TOTAL_STEPS=2");
    }

    @Test
    @DisplayName("Loop with maxSteps=1 terminates immediately with STEP_LIMIT")
    void loopWithMaxStepsOneTerminatesImmediately() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(1);

        SingleTurnExecutor executor = (prompt) -> TurnResult.withoutTool("single step");

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("anything");

        assertThat(result.steps()).hasSize(1);

        String aggregated = result.aggregated();
        assertThat(aggregated).contains("TERMINATED=true");
        assertThat(aggregated).contains("REASON=STEP_LIMIT");
        assertThat(aggregated).contains("TOTAL_STEPS=1");
    }

    @Test
    @DisplayName("Termination reason defaults to null when loop not yet terminated (edge case)")
    void nonTerminatedLoopHasNullReason() {
        // This test verifies ExecutionContext structure semantics
        // Actual loop implementation always terminates (STEP_LIMIT or COMPLETED)
        // But ExecutionContext should support non-terminated state

        ExecutionContext ctx = new ExecutionContext(0, 10, false, null);

        assertThat(ctx.terminated()).isFalse();
        assertThat(ctx.reason()).isNull();
    }

    @Test
    @DisplayName("COMPLETED termination takes precedence over STEP_LIMIT when both conditions met simultaneously")
    void completedTakesPrecedenceOverStepLimit() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(2);

        // Executor signals COMPLETED on the exact step that would hit maxSteps
        int[] callCount = {0};
        SingleTurnExecutor executor = (prompt) -> {
            callCount[0]++;
            if (callCount[0] == 2) {
                return TurnResult.withoutTool("TERMINATION_SIGNAL:COMPLETED");
            }
            return TurnResult.withoutTool("step result");
        };

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("test");

        // Verify COMPLETED takes precedence (implementation decision)
        String aggregated = result.aggregated();
        assertThat(aggregated).contains("TERMINATED=true");
        assertThat(aggregated).contains("REASON=COMPLETED");
        assertThat(aggregated).contains("TOTAL_STEPS=2");
    }
}
