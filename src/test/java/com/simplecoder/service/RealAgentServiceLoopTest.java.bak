package com.simplecoder.service;

import com.simplecoder.config.AgentLoopProperties;
import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R-11/R-12: Real loop execution tests using actual AgentLoopService + AgentService
 * with a deterministic SingleTurnExecutor test double (no Spring context needed).
 */
public class RealAgentServiceLoopTest {

    private AgentService newAgent(int maxSteps, Queue<String> scriptedOutputs) {
        AgentLoopProperties props = new AgentLoopProperties();
        props.setMaxSteps(maxSteps);
        // SingleTurnExecutor pulls next scripted output each call; if empty returns "idle".
        SingleTurnExecutor exec = prompt -> scriptedOutputs.isEmpty() 
            ? TurnResult.withoutTool("idle") 
            : TurnResult.withoutTool(scriptedOutputs.poll());
        AgentLoopService loop = new AgentLoopService(props, exec);
        return new AgentService(loop);
    }

    private ToolRequest req(String prompt) {
        return new ToolRequest(prompt, "auto", List.of());
    }

    @Test
    @DisplayName("RealLoop: COMPLETED terminates early before step limit")
    void completedTerminatesEarly() {
        Queue<String> q = new ArrayDeque<>();
        q.add("phase one");
        q.add("doing work TERMINATION_SIGNAL:COMPLETED success");
        q.add("should never be used");
        AgentService agent = newAgent(5, q);
        ToolResponse resp = agent.process(req("build project"));
        String data = (String) resp.getData();
        assertTrue(data.contains("REASON=COMPLETED"));
        assertTrue(data.contains("STEP 1"));
        assertTrue(data.contains("STEP 2"));
        assertFalse(data.contains("STEP 3"), "Should not execute after COMPLETED");
        assertTrue(data.contains("TOTAL_STEPS=2"));
    }

    @Test
    @DisplayName("RealLoop: STEP_LIMIT reached when no completion signal")
    void stepLimitReached() {
        Queue<String> q = new ArrayDeque<>();
        q.add("a"); q.add("b"); q.add("c");
        AgentService agent = newAgent(3, q);
        ToolResponse resp = agent.process(req("run steps"));
        String data = (String) resp.getData();
        assertTrue(data.contains("REASON=STEP_LIMIT"));
        assertTrue(data.contains("STEP 1"));
        assertTrue(data.contains("STEP 2"));
        assertTrue(data.contains("STEP 3"));
        assertTrue(data.contains("TOTAL_STEPS=3"));
    }

    @Test
    @DisplayName("RealLoop: Aggregated line count within expected bound for maxSteps=10")
    void aggregatedLineCountBound() {
        Queue<String> q = new ArrayDeque<>();
        for (int i = 1; i <= 10; i++) {
            q.add("iteration-" + i);
        }
        AgentService agent = newAgent(10, q);
        ToolResponse resp = agent.process(req("long run"));
        String data = (String) resp.getData();
        assertTrue(data.contains("TOTAL_STEPS=10"));
        // Count lines
        int lines = data.split("\r?\n").length;
        // Expect: 1 header + 10 step lines + 1 total = 12
        assertEquals(12, lines, "Aggregated output should have 12 lines");
    }
}
