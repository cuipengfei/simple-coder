package com.simplecoder.service;

import com.simplecoder.config.AgentLoopProperties;
import com.simplecoder.exception.RecoverableException;
import com.simplecoder.exception.TerminalException;
import com.simplecoder.exception.ValidationException;
import com.simplecoder.exception.SecurityViolationException;
import com.simplecoder.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * R-8 exception handling tests (written before implementation).
 *
 * Tests two exception categories:
 * 1. RecoverableException: Loop continues, exception message becomes observation
 * 2. TerminalException: Loop terminates immediately with termination reason
 *
 * Verification points per ROADMAP R-8:
 * - Recoverable exceptions allow loop to continue
 * - Terminal exceptions abort loop immediately
 * - Exception messages captured in ExecutionStep or termination reason
 * - Step counter behavior correct for both cases
 */
class AgentLoopExceptionHandlingTest {

    @Test
    @DisplayName("Loop continues when SingleTurnExecutor throws RecoverableException")
    void loopContinuesOnRecoverableException() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(5);

        // Executor throws RecoverableException on step 2, then continues normally
        int[] callCount = {0};
        SingleTurnExecutor executor = (prompt) -> {
            callCount[0]++;
            if (callCount[0] == 2) {
                throw new ValidationException("filePath", "File not found: missing.txt");
            }
            return TurnResult.withoutTool("normal step result " + callCount[0]);
        };

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("test recoverable");

        // Verify loop executed all maxSteps (did not terminate early)
        assertThat(result.steps()).hasSize(5);

        // Verify step 2 contains error message as observation
        ExecutionStep step2 = result.steps().get(1); // 0-indexed
        assertThat(step2.resultSummary()).contains("Validation failed for 'filePath': File not found: missing.txt");

        // Verify loop terminated normally with STEP_LIMIT (not error termination)
        String aggregated = result.aggregated();
        assertThat(aggregated).contains("TERMINATED=true");
        assertThat(aggregated).contains("REASON=STEP_LIMIT");
        assertThat(aggregated).contains("TOTAL_STEPS=5");
    }

    @Test
    @DisplayName("Loop terminates immediately when SingleTurnExecutor throws TerminalException")
    void loopTerminatesOnTerminalException() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(10);

        // Executor throws TerminalException on step 3
        int[] callCount = {0};
        SingleTurnExecutor executor = (prompt) -> {
            callCount[0]++;
            if (callCount[0] == 3) {
                throw new SecurityViolationException("path_escape", "Attempted to access /etc/passwd");
            }
            return TurnResult.withoutTool("normal step result " + callCount[0]);
        };

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("test terminal");

        // Verify loop stopped early (only 3 steps, not maxSteps=10)
        assertThat(result.steps()).hasSize(3);

        // Verify step 3 contains error indication
        ExecutionStep step3 = result.steps().get(2); // 0-indexed
        assertThat(step3.resultSummary()).contains("SECURITY_VIOLATION");

        // Verify loop terminated with SECURITY_VIOLATION reason
        String aggregated = result.aggregated();
        assertThat(aggregated).contains("TERMINATED=true");
        assertThat(aggregated).contains("REASON=SECURITY_VIOLATION");
        assertThat(aggregated).contains("TOTAL_STEPS=3");
    }

    @Test
    @DisplayName("Multiple RecoverableExceptions in sequence all captured as observations")
    void multipleRecoverableExceptionsContinueLoop() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(4);

        // Executor throws RecoverableException on steps 1 and 3
        int[] callCount = {0};
        SingleTurnExecutor executor = (prompt) -> {
            callCount[0]++;
            if (callCount[0] == 1) {
                throw new ValidationException("pattern", "Empty regex pattern");
            }
            if (callCount[0] == 3) {
                throw new ValidationException("pattern", "No matches found");
            }
            return TurnResult.withoutTool("normal step result " + callCount[0]);
        };

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("test multiple recoverable");

        // Verify all steps executed (loop did not terminate early)
        assertThat(result.steps()).hasSize(4);

        // Verify step 1 contains first error
        ExecutionStep step1 = result.steps().get(0);
        assertThat(step1.resultSummary()).contains("Validation failed for 'pattern': Empty regex pattern");

        // Verify step 3 contains second error
        ExecutionStep step3 = result.steps().get(2);
        assertThat(step3.resultSummary()).contains("Validation failed for 'pattern': No matches found");

        // Verify loop terminated normally with STEP_LIMIT
        String aggregated = result.aggregated();
        assertThat(aggregated).contains("REASON=STEP_LIMIT");
    }

    @Test
    @DisplayName("TerminalException on first step terminates immediately")
    void terminalExceptionOnFirstStepTerminatesImmediately() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(5);

        // Executor throws TerminalException immediately on step 1
        SingleTurnExecutor executor = (prompt) -> {
            throw new SecurityViolationException("malicious_input", "SQL injection detected");
        };

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("test immediate terminal");

        // Verify only 1 step executed (terminated immediately)
        assertThat(result.steps()).hasSize(1);

        // Verify step 1 contains error indication
        ExecutionStep step1 = result.steps().get(0);
        assertThat(step1.resultSummary()).contains("SECURITY_VIOLATION");

        // Verify termination reason
        String aggregated = result.aggregated();
        assertThat(aggregated).contains("TERMINATED=true");
        assertThat(aggregated).contains("REASON=SECURITY_VIOLATION");
        assertThat(aggregated).contains("TOTAL_STEPS=1");
    }

    @Test
    @DisplayName("RecoverableException followed by TerminalException shows correct termination reason")
    void recoverableThenTerminalShowsCorrectReason() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(10);

        // Executor throws RecoverableException on step 1, TerminalException on step 4
        int[] callCount = {0};
        SingleTurnExecutor executor = (prompt) -> {
            callCount[0]++;
            if (callCount[0] == 1) {
                throw new ValidationException("dirPath", "Directory not found");
            }
            if (callCount[0] == 4) {
                throw new TerminalException("SYSTEM_ERROR", "Out of memory");
            }
            return TurnResult.withoutTool("normal step result " + callCount[0]);
        };

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("test mixed exceptions");

        // Verify loop stopped at step 4 (when TerminalException thrown)
        assertThat(result.steps()).hasSize(4);

        // Verify step 1 has recoverable error observation
        ExecutionStep step1 = result.steps().get(0);
        assertThat(step1.resultSummary()).contains("Validation failed for 'dirPath': Directory not found");

        // Verify step 4 has terminal error indication
        ExecutionStep step4 = result.steps().get(3);
        assertThat(step4.resultSummary()).contains("SYSTEM_ERROR");

        // Verify final termination reason is SYSTEM_ERROR (not STEP_LIMIT)
        String aggregated = result.aggregated();
        assertThat(aggregated).contains("TERMINATED=true");
        assertThat(aggregated).contains("REASON=SYSTEM_ERROR");
        assertThat(aggregated).contains("TOTAL_STEPS=4");
    }

    @Test
    @DisplayName("Exception messages are truncated in ExecutionStep summary")
    void exceptionMessagesTruncatedInStepSummary() {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(1);

        // Executor throws RecoverableException with very long message
        String longMessage = "A".repeat(200); // 200 character error message
        SingleTurnExecutor executor = (prompt) -> {
            throw new ValidationException("testParam", longMessage);
        };

        AgentLoopService loopService = new AgentLoopService(props, executor);
        LoopResult result = loopService.runLoop("test truncation");

        // Verify step contains truncated error message (per existing truncate logic)
        ExecutionStep step1 = result.steps().get(0);
        // Current truncation logic in AgentLoopService: truncate(raw, 80)
        assertThat(step1.resultSummary().length()).isLessThanOrEqualTo(80);
        assertThat(step1.resultSummary()).startsWith("Validation failed for 'testParam':");
    }
}
