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

            // Let Spring AI ChatClient handle tool selection and parameter extraction automatically
            String result = chatClient.prompt()
                    .user(promptBuilder.toString())
                    .tools(toolsService)  // Register all @Tool annotated methods
                    .call()
                    .content();

            log.info("Tool execution completed successfully");
            return ToolResponse.success("Tool execution result", result);

        } catch (Exception e) {
            log.error("Failed to process ToolRequest", e);
            return ToolResponse.error("AgentService error", e.getMessage());
        }
    }
}
