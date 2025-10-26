package com.simplecoder.service;

import com.simplecoder.config.AgentLoopProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies early stop now uses truncated display summary.
 * Raw outputs differ after 80 chars but first 80 are identical.
 * Should terminate after second step.
 */
class DisplayBasedRepetitionEarlyStopTest {

    @Test
    @DisplayName("Display repetition triggers early COMPLETED termination")
    void displayRepetitionStopsEarly() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(6);

        int[] count = {0};
        SingleTurnExecutor executor = prompt -> {
            count[0]++;
            // First 80 chars identical: "PREFIX:" + 71 'A' (total 78) + 'X' + 'Y' = 80 \n
            // After that we append differing suffix per call
            String base = "PREFIX:" + "A".repeat(71) + "XY"; // length 80
            String suffix = switch (count[0]) {
                case 1 -> " first suffix";
                case 2 -> " second suffix"; // repetition triggers early stop at step 2 (display identical)
                default -> " another suffix";
            };
            return TurnResult.withoutTool(base + suffix);
        };

        AgentLoopService service = new AgentLoopService(props, executor);
        LoopResult result = service.runLoop("irrelevant prompt");

        assertEquals(2, result.steps().size());
        assertEquals("COMPLETED", result.finalContext().reason());
    }
}
