package com.simplecoder.service;

import com.simplecoder.model.ContextEntry;
import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

/**
 * R-11 RED PHASE TESTS (will FAIL before AgentService loop integration).
 *
 * Intent: Specify expected aggregated multi-step output format once AgentService
 * delegates to AgentLoopService. Current AgentService is single-turn, so these
 * assertions should fail (RED) until integration is implemented.
 */
public class AgentServiceLoopIntegrationTest {

    private ToolRequest newRequest(String prompt) {
        return new ToolRequest(prompt, "auto", List.of(
                ContextEntry.builder().prompt("prev").result("res").build()
        ));
    }

    private AgentService buildAgentWithStubbedLoop(String aggregated, int totalSteps, String reason) {
        LoopResult loopResult = new LoopResult(List.of(), aggregated,
                new com.simplecoder.model.ExecutionContext(totalSteps, totalSteps, true, reason));
        AgentLoopService stubLoop = new AgentLoopService(null, prompt -> TurnResult.withoutTool("unused")) {
            @Override
            public LoopResult runLoop(String initialPrompt) {
                return loopResult;
            }
        };
        return new AgentService(stubLoop);
    }

    @Test
    @DisplayName("R-11: singleStepCompatibility_maxSteps1 expects aggregated header and STEP 1 line")
    void singleStepCompatibility_maxSteps1() {
        String agg = "HEADER: STEP n | tool=<tool> | summary=\"...\" | tasks=total:P/IP/C" +
                System.lineSeparator() +
                "STEP 1 | tool=- | summary=\"work\" | tasks=0:0P/0IP/0C" +
                System.lineSeparator() +
                "TOTAL_STEPS=1 TERMINATED=true REASON=STEP_LIMIT";
        AgentService agent = buildAgentWithStubbedLoop(agg,1,"STEP_LIMIT");
        ToolResponse resp = agent.process(newRequest("do something"));
        String text = (String) resp.getData();
        assertTrue(text.contains("HEADER:"));
        assertTrue(text.contains("STEP 1"));
        assertTrue(text.contains("TOTAL_STEPS=1"));
    }

    @Test
    @DisplayName("R-11: stepLimitReached_reasonStepLimit expects REASON=STEP_LIMIT and multiple STEP lines")
    void stepLimitReached_reasonStepLimit() {
        String agg = "HEADER: STEP n | tool=<tool> | summary=\"...\" | tasks=total:P/IP/C" +
                System.lineSeparator() +
                "STEP 1 | tool=- | summary=\"alpha\" | tasks=0:0P/0IP/0C" +
                System.lineSeparator() +
                "STEP 2 | tool=- | summary=\"beta\" | tasks=0:0P/0IP/0C" +
                System.lineSeparator() +
                "STEP 3 | tool=- | summary=\"gamma\" | tasks=0:0P/0IP/0C" +
                System.lineSeparator() +
                "TOTAL_STEPS=3 TERMINATED=true REASON=STEP_LIMIT";
        AgentService agent = buildAgentWithStubbedLoop(agg,3,"STEP_LIMIT");
        ToolResponse resp = agent.process(newRequest("run three"));
        String text = (String) resp.getData();
        assertTrue(text.contains("REASON=STEP_LIMIT"));
        assertTrue(text.contains("STEP 1"));
        assertTrue(text.contains("STEP 2"));
        assertTrue(text.contains("STEP 3"));
    }

    @Test
    @DisplayName("R-11: completedSignalShortCircuits expects REASON=COMPLETED and only two steps")
    void completedSignalShortCircuits() {
        String agg = "HEADER: STEP n | tool=<tool> | summary=\"...\" | tasks=total:P/IP/C" +
                System.lineSeparator() +
                "STEP 1 | tool=- | summary=\"work chunk A\" | tasks=0:0P/0IP/0C" +
                System.lineSeparator() +
                "STEP 2 | tool=- | summary=\"some stuff COMPLETED\" | tasks=0:0P/0IP/0C" +
                System.lineSeparator() +
                "TOTAL_STEPS=2 TERMINATED=true REASON=COMPLETED";
        AgentService agent = buildAgentWithStubbedLoop(agg,2,"COMPLETED");
        ToolResponse resp = agent.process(newRequest("finish early"));
        String text = (String) resp.getData();
        assertTrue(text.contains("REASON=COMPLETED"));
        assertTrue(text.contains("STEP 1"));
        assertTrue(text.contains("STEP 2"));
        assertFalse(text.contains("STEP 3"));
    }
}
