package com.simplecoder.service;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import com.simplecoder.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Core stateless agent service: single-turn processing of a ToolRequest.
 */
@Slf4j
@Service
public class AgentService {

    private final Map<String, Tool> toolsByName; // injected map of tool beans
    private final ToolSelectionStrategy toolSelectionStrategy;

    public AgentService(Map<String, Tool> toolsByName, ToolSelectionStrategy toolSelectionStrategy) {
        this.toolsByName = toolsByName;
        this.toolSelectionStrategy = toolSelectionStrategy;
    }

    /**
     * Process a single ToolRequest.
     * @param request tool request
     * @return ToolResponse from executed tool
     */
    public ToolResponse process(ToolRequest request) {
        try {
            request.validate();
            String explicitType = request.getToolType();
            String selected = explicitType;

            if (explicitType == null || explicitType.isBlank()) {
                explicitType = "auto"; // normalize
            }

            if ("auto".equalsIgnoreCase(explicitType)) {
                selected = toolSelectionStrategy.selectTool(request);
                log.info("Auto tool selection -> {}", selected);
            }

            Tool tool = toolsByName.get(selected);
            if (tool == null) {
                // Fallback: search by Tool.getName() in case map keys are bean names (e.g. readFileTool)
                for (Tool t : toolsByName.values()) {
                    if (t.getName() != null && selected != null && selected.equalsIgnoreCase(t.getName())) {
                        tool = t;
                        break;
                    }
                }
            }
            if (tool == null) {
                return ToolResponse.error("Unknown tool: " + selected);
            }

            ToolResponse resp = tool.execute(request);
            // Prefix message with tool name for traceability
            if (resp != null && resp.getMessage() != null) {
                resp.setMessage(String.format("[tool=%s] %s", selected, resp.getMessage()));
            }
            return resp;
        } catch (Exception e) {
            log.error("Failed to process ToolRequest", e);
            return ToolResponse.error("AgentService error", e.getMessage());
        }
    }
}
