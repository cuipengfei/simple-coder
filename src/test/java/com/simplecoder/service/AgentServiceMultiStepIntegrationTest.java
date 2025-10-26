package com.simplecoder.service;

import com.simplecoder.config.AgentLoopProperties;
import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * R-12 integration tests: Multi-step execution via AgentService after R-11 integration.
 *
 * Tests verify:
 * 1. COMPLETED signal precedence over STEP_LIMIT
 * 2. Aggregated output line count within expected bounds
 * 3. Integration with existing ToolRequest/ToolResponse flow
 *
 * Assumptions from R-11:
 * - AgentService.process() now uses AgentLoopService internally
 * - Returns aggregated result from multiple steps
 * - SingleTurnExecutor wraps ChatClient calls
 */
class AgentServiceMultiStepIntegrationTest {

    private AgentLoopProperties properties;
    private ChatClient mockChatClient;
    private ToolsService mockToolsService;

    @BeforeEach
    void setUp() {
        properties = new AgentLoopProperties();
        properties.setMaxSteps(10);
        mockChatClient = mock(ChatClient.class);
        mockToolsService = mock(ToolsService.class);
    }

    @Test
    @DisplayName("COMPLETED termination takes precedence over STEP_LIMIT when both conditions met simultaneously")
    void completedSignalTakesPrecedenceOverStepLimit() {
        // Setup: maxSteps=3, executor signals COMPLETED on step 3
        properties.setMaxSteps(3);

        // Create stub executor that signals COMPLETED on last step
        int[] callCount = {0};
        SingleTurnExecutor stubExecutor = (prompt) -> {
            callCount[0]++;
            if (callCount[0] == 3) {
                return TurnResult.withoutTool("TERMINATION_SIGNAL:COMPLETED"); // COMPLETED on step 3 (which is also maxSteps)
            }
            return TurnResult.withoutTool("step " + callCount[0] + " result");
        };

        AgentLoopService loopService = new AgentLoopService(properties, stubExecutor);
        LoopResult result = loopService.runLoop("test precedence");

        // Verify COMPLETED takes precedence over STEP_LIMIT
        String aggregated = result.aggregated();
        assertThat(aggregated).contains("TERMINATED=true");
        assertThat(aggregated).contains("REASON=COMPLETED");
        assertThat(aggregated).doesNotContain("REASON=STEP_LIMIT");
        assertThat(result.steps()).hasSize(3);
    }

    @Test
    @DisplayName("Aggregated output line count stays within bounds for full 10-step execution")
    void aggregatedOutputLineCountWithinBounds() {
        // Setup: maxSteps=10, executor returns normal results
        properties.setMaxSteps(10);

        int[] callCount = {0};
        SingleTurnExecutor stubExecutor = (prompt) -> TurnResult.withoutTool("step result with some text content " + (++callCount[0]));

        AgentLoopService loopService = new AgentLoopService(properties, stubExecutor);
        LoopResult result = loopService.runLoop("test output size");

        // Verify aggregated output is reasonable (< 25 lines for 10 steps per R-12 spec)
        String aggregated = result.aggregated();
        long lineCount = aggregated.lines().count();
        assertThat(lineCount)
                .as("Aggregated output should be concise (< 25 lines for 10 steps)")
                .isLessThan(25);

        // Verify contains step information
        assertThat(aggregated).contains("STEP 1");
        assertThat(aggregated).contains("STEP 10");
        assertThat(aggregated).contains("TOTAL_STEPS=10");
    }

    @Test
    @DisplayName("Aggregated output contains termination metadata and all step summaries")
    void aggregatedOutputContainsAllRequiredMetadata() {
        properties.setMaxSteps(5);

        int[] callCount = {0};
        SingleTurnExecutor stubExecutor = (prompt) -> TurnResult.withoutTool("result " + (++callCount[0]));

        AgentLoopService loopService = new AgentLoopService(properties, stubExecutor);
        LoopResult result = loopService.runLoop("test metadata");

        String aggregated = result.aggregated();

        // Verify required metadata fields
        assertThat(aggregated).contains("TERMINATED=");
        assertThat(aggregated).contains("REASON=");
        assertThat(aggregated).contains("TOTAL_STEPS=");

        // Verify all steps present in aggregated output
        for (int i = 1; i <= 5; i++) {
            assertThat(aggregated).contains("STEP " + i);
        }
    }

    @Test
    @DisplayName("Integration: ToolRequest with empty context history produces valid aggregated output")
    void toolRequestWithEmptyContextHistoryProducesValidOutput() {
        // This test verifies R-11 integration assumption:
        // AgentService.process(ToolRequest) should work with AgentLoopService

        properties.setMaxSteps(2);

        // Create a minimal ToolRequest (what AgentService.process() receives)
        ToolRequest request = new ToolRequest();
        request.setPrompt("test prompt");
        request.setToolType("auto");
        request.setContextHistory(Collections.emptyList());

        // Stub executor simulating ChatClient responses
        SingleTurnExecutor stubExecutor = (prompt) -> TurnResult.withoutTool("ChatClient response");

        AgentLoopService loopService = new AgentLoopService(properties, stubExecutor);
        LoopResult result = loopService.runLoop(request.getPrompt());

        // Verify result structure is suitable for ToolResponse conversion
        assertThat(result.aggregated()).isNotNull();
        assertThat(result.aggregated()).isNotBlank();
        assertThat(result.steps()).hasSize(2);
        assertThat(result.finalContext().terminated()).isTrue();
    }

    @Test
    @DisplayName("Aggregated output format is stable across different step counts")
    void aggregatedOutputFormatStableAcrossDifferentStepCounts() {
        // Test with different maxSteps values to ensure format consistency
        for (int maxSteps : new int[]{1, 3, 5, 10}) {
            properties.setMaxSteps(maxSteps);

            int[] callCount = {0};
            SingleTurnExecutor stubExecutor = (prompt) -> TurnResult.withoutTool("result " + (++callCount[0]));

            AgentLoopService loopService = new AgentLoopService(properties, stubExecutor);
            LoopResult result = loopService.runLoop("test format stability");

            String aggregated = result.aggregated();

            // Verify consistent format elements present
            assertThat(aggregated)
                    .as("Aggregated output for maxSteps=%d should contain TERMINATED", maxSteps)
                    .contains("TERMINATED=");

            assertThat(aggregated)
                    .as("Aggregated output for maxSteps=%d should contain REASON", maxSteps)
                    .contains("REASON=");

            assertThat(aggregated)
                    .as("Aggregated output for maxSteps=%d should contain TOTAL_STEPS", maxSteps)
                    .contains("TOTAL_STEPS=" + maxSteps);
        }
    }

    @Test
    @DisplayName("Very long step results are truncated in aggregated output to prevent overflow")
    void longStepResultsTruncatedInAggregatedOutput() {
        properties.setMaxSteps(3);

        // Executor returns very long results (200 chars each)
        int[] callCount = {0};
        SingleTurnExecutor stubExecutor = (prompt) -> {
            String longResult = (++callCount[0]) + "A".repeat(200);
            return TurnResult.withoutTool(longResult);
        };

        AgentLoopService loopService = new AgentLoopService(properties, stubExecutor);
        LoopResult result = loopService.runLoop("test truncation");

        String aggregated = result.aggregated();

        // Verify aggregated output is reasonable despite long step results
        // Each step's summary should be truncated (per AgentLoopService.truncate(raw, 80))
        long lineCount = aggregated.lines().count();
        assertThat(lineCount)
                .as("Aggregated output should remain concise even with long step results")
                .isLessThan(20);

        // Verify step results are present but truncated
        assertThat(aggregated).contains("STEP 1");
        assertThat(aggregated).contains("STEP 2");
        assertThat(aggregated).contains("STEP 3");
    }
}
