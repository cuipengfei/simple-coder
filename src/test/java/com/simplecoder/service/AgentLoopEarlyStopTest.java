package com.simplecoder.service;

import com.simplecoder.config.AgentLoopProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IF-2: Early stop logic via repetition detection.
 * Verifies that the loop terminates early when consecutive steps produce identical normalized summaries.
 */
class AgentLoopEarlyStopTest {

    @Test
    @DisplayName("IF-2a: Repetition detection stops loop early with COMPLETED reason")
    void repetitionDetectionStopsEarly() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(5);

        // Executor returns identical response each time
        SingleTurnExecutor executor = prompt -> TurnResult.withoutTool("Same answer every time.");
        
        AgentLoopService service = new AgentLoopService(props, executor);
        LoopResult result = service.runLoop("test prompt");

        // Should stop after 2 steps (first + detected repeat)
        assertEquals(2, result.steps().size());
        assertTrue(result.finalContext().terminated());
        assertEquals("COMPLETED", result.finalContext().reason());
    }

    @Test
    @DisplayName("IF-2b: Punctuation differences ignored in repetition detection")
    void punctuationIgnoredInRepetition() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(5);

        int[] callCount = {0};
        SingleTurnExecutor executor = prompt -> {
            callCount[0]++;
            return callCount[0] == 1 
                ? TurnResult.withoutTool("Answer: SimpleCoderApplication is the entry point!") 
                : TurnResult.withoutTool("answer simplecoderapplication is the entry point");
        };
        
        AgentLoopService service = new AgentLoopService(props, executor);
        LoopResult result = service.runLoop("what is the main class?");

        // Should stop after 2 steps (normalized versions are identical)
        assertEquals(2, result.steps().size());
        assertEquals("COMPLETED", result.finalContext().reason());
    }

    @Test
    @DisplayName("IF-2c: Different answers prevent early termination until max steps")
    void differentAnswersReachStepLimit() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(3);

        int[] callCount = {0};
        SingleTurnExecutor executor = prompt -> {
            callCount[0]++;
            return TurnResult.withoutTool("Step " + callCount[0] + " unique output");
        };
        
        AgentLoopService service = new AgentLoopService(props, executor);
        LoopResult result = service.runLoop("test");

        // Should reach max steps
        assertEquals(3, result.steps().size());
        assertEquals("STEP_LIMIT", result.finalContext().reason());
    }

    @Test
    @DisplayName("IF-2d: TERMINATION_SIGNAL still takes precedence over repetition")
    void terminationSignalTakesPrecedence() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(5);

        SingleTurnExecutor executor = prompt -> TurnResult.withoutTool("TERMINATION_SIGNAL:COMPLETED");
        
        AgentLoopService service = new AgentLoopService(props, executor);
        LoopResult result = service.runLoop("test");

        // Should stop after 1 step due to explicit signal
        assertEquals(1, result.steps().size());
        assertEquals("COMPLETED", result.finalContext().reason());
    }

    @Test
    @DisplayName("IF-2e: Repetition detection applies to consecutive steps only")
    void repetitionRequiresConsecutiveSteps() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(5);

        int[] callCount = {0};
        SingleTurnExecutor executor = prompt -> {
            callCount[0]++;
            // Pattern: A, B, A (not consecutive, so no early stop)
            return switch (callCount[0]) {
                case 1 -> TurnResult.withoutTool("First answer");
                case 2 -> TurnResult.withoutTool("Different second answer");
                case 3 -> TurnResult.withoutTool("First answer"); // same as #1 but not consecutive
                default -> TurnResult.withoutTool("More output " + callCount[0]);
            };
        };
        
        AgentLoopService service = new AgentLoopService(props, executor);
        LoopResult result = service.runLoop("test");

        // Should reach max steps (repetition not consecutive)
        assertEquals(5, result.steps().size());
        assertEquals("STEP_LIMIT", result.finalContext().reason());
    }

    @Test
    @DisplayName("IF-2f: Subtle formatting differences normalized correctly (real-world AI model behavior)")
    void subtleFormattingNormalized() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(5);

        int[] callCount = {0};
        SingleTurnExecutor executor = prompt -> {
            callCount[0]++;
            // Simulate real AI responses: semantically identical but with formatting variations
            return switch (callCount[0]) {
                case 1 -> TurnResult.withoutTool("这个仓库的 Java 主入口类是：com.simplecoder.SimpleCoderApplication，也就是 src/main/java/com/si");
                case 2 -> TurnResult.withoutTool("这个仓库的 Java 主入口类是：com.simplecoder.SimpleCoderApplication。");
                case 3 -> TurnResult.withoutTool("这个仓库的 Java 主入口类是：com.simplecoder.SimpleCoderApplication。  ");
                default -> TurnResult.withoutTool("这个仓库的Java主入口类是 com.simplecoder.SimpleCoderApplication ");
            };
        };
        
        AgentLoopService service = new AgentLoopService(props, executor);
        LoopResult result = service.runLoop("which class is the java main entry point");

        // Steps 2 and 3 should normalize identically despite trailing period difference
        // BUT step 1 is different (truncated at "si"), so should stop at step 3 (when 2==3)
        assertEquals(3, result.steps().size());
        assertEquals("COMPLETED", result.finalContext().reason());
    }

    @Test
    @DisplayName("IF-2g: Identical semantic content with only whitespace/punctuation differences stops early")
    void whitespaceOnlyDifferencesDetected() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(5);

        int[] callCount = {0};
        SingleTurnExecutor executor = prompt -> {
            callCount[0]++;
            // Same content, different whitespace/punctuation
            return switch (callCount[0]) {
                case 1 -> TurnResult.withoutTool("SimpleCoderApplication is the entry point!");
                case 2 -> TurnResult.withoutTool("SimpleCoderApplication is the entry point!  ");
                default -> TurnResult.withoutTool("  SimpleCoderApplication is the entry point ! ");
            };
        };
        
        AgentLoopService service = new AgentLoopService(props, executor);
        LoopResult result = service.runLoop("what is main class");

        // Should stop at step 2 (first + immediate repeat with whitespace diff)
        assertEquals(2, result.steps().size());
        assertEquals("COMPLETED", result.finalContext().reason());
    }
}
