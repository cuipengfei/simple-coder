package com.simplecoder.service;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Core stateless agent service.
 * R-11 / R-12 full integration: always uses multi-step loop (AgentLoopService)
 * to process requests. Legacy single-turn path removed; single-step behavior
 * still achievable by configuring maxSteps=1.
 */
@Slf4j
@Service
public class AgentService {

    private final AgentLoopService agentLoopService;

    public AgentService(AgentLoopService agentLoopService) {
        this.agentLoopService = agentLoopService;
    }

    public ToolResponse process(ToolRequest request) {
        try {
            request.validate();
            String contextSummary = request.buildContextSummary();
            StringBuilder promptBuilder = new StringBuilder();
            if (contextSummary != null && !contextSummary.isBlank()) {
                promptBuilder.append("Context History:\n").append(contextSummary).append("\n\n");
            }
            promptBuilder.append("User Request:\n").append(request.getPrompt());
            String combinedPrompt = promptBuilder.toString();
            var loopResult = agentLoopService.runLoop(combinedPrompt);
            return ToolResponse.success("Tool execution result", loopResult.aggregated());
        } catch (Exception e) {
            log.error("Failed to process ToolRequest", e);
            return ToolResponse.error("AgentService error", e.getMessage());
        }
    }
}
