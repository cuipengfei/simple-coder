package com.simplecoder.service;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Core stateless agent service: single-turn processing of a ToolRequest.
 * Now uses Spring AI native tool calling with @Tool annotated methods.
 */
@Slf4j
@Service
public class AgentService {

    private final ChatClient chatClient;
    private final ToolsService toolsService;

    public AgentService(ChatClient chatClient, ToolsService toolsService) {
        this.chatClient = chatClient;
        this.toolsService = toolsService;
    }

    /**
     * Process a single ToolRequest using Spring AI's native tool calling.
     * The model automatically selects the appropriate tool and extracts parameters from natural language.
     *
     * <p>Core mechanism:
     * 1. ChatClient sends user prompt to LLM
     * 2. LLM analyzes prompt and selects appropriate @Tool method from ToolsService
     * 3. LLM extracts parameter values from natural language (e.g., "read file X lines 1-10")
     * 4. Spring AI invokes the selected tool method with extracted parameters
     * 5. Tool execution result is returned as string
     *
     * @param request tool request with natural language prompt
     * @return ToolResponse with tool execution result
     */
    public ToolResponse process(ToolRequest request) {
        try {
            request.validate();

            String contextSummary = request.buildContextSummary();
            StringBuilder promptBuilder = new StringBuilder();

            if (contextSummary != null && !contextSummary.isBlank()) {
                promptBuilder.append("Context History:\n").append(contextSummary).append("\n\n");
            }

            promptBuilder.append("User Request:\n").append(request.getPrompt());

            log.debug("Processing request with prompt length: {} chars", promptBuilder.length());

            String result = chatClient.prompt()
                    .user(promptBuilder.toString())
                    .tools(toolsService)
                    .call()
                    .content();
            // Check if tool execution failed (tools return "Error: ..." prefix on failure)
            if (result != null && result.trim().startsWith("Error:")) {
                log.warn("Tool execution returned error: {}", result.substring(0, Math.min(200, result.length())));
                return ToolResponse.error("Tool execution failed", result);
            }

            return ToolResponse.success("Tool execution result", result);

        } catch (Exception e) {
            log.error("Failed to process ToolRequest", e);
            return ToolResponse.error("AgentService error", e.getMessage());
        }
    }
}
