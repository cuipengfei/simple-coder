package com.simplecoder.service;

import com.simplecoder.config.AgentLoopProperties;
import com.simplecoder.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R-6 loop skeleton tests (written before implementation).
 */
class AgentLoopServiceTest {

    @Test
    @DisplayName("Loop stops at configured maxSteps")
    void runLoopStopsAtMaxSteps() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(2);

        // Simple stub executor: returns constant summary each call
        SingleTurnExecutor executor = (prompt) -> TurnResult.withoutTool("CONST");

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("user prompt");

        assertThat(result.steps()).hasSize(2);
        assertThat(result.aggregated()).contains("STEP 1").contains("STEP 2");
        assertThat(result.aggregated()).doesNotContain("STEP 3");
    }

    @Test
    @DisplayName("Step numbers increment sequentially starting at 1")
    void runLoopIncrementsStepCount() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(3);

        int[] callCount = {0};
        SingleTurnExecutor executor = (prompt) -> TurnResult.withoutTool("VALUE " + (++callCount[0]));
        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("anything");

        List<ExecutionStep> steps = result.steps();
        assertThat(steps).hasSize(3);
        assertThat(steps.get(0).stepNumber()).isEqualTo(1);
        assertThat(steps.get(1).stepNumber()).isEqualTo(2);
        assertThat(steps.get(2).stepNumber()).isEqualTo(3);
    }
}
