package com.simplecoder.service;

import com.simplecoder.model.ToolRequest;
import com.simplecoder.model.ToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Core stateless agent service.
 * Uses Spring AI's built-in ReAct loop via ChatClient with registered tools.
 * The AI model automatically decides when to call tools, how many times, and when to return final answer.
 * This replaces the previous manual multi-step loop with the framework's native agentic behavior.
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

    public ToolResponse process(ToolRequest request) {
        try {
            request.validate();

            // Build prompt with context history if present
            String contextSummary = request.buildContextSummary();
            StringBuilder promptBuilder = new StringBuilder();
            if (contextSummary != null && !contextSummary.isBlank()) {
                promptBuilder.append("Context History:\n").append(contextSummary).append("\n\n");
            }
            promptBuilder.append("User Request:\n").append(request.getPrompt());

            // Single ChatClient call - Spring AI handles the entire ReAct loop internally:
            // 1. AI decides which tools to call (if any)
            // 2. Framework executes tools automatically
            // 3. AI sees tool results and decides next action (more tools or final answer)
            // 4. Returns when AI determines task is complete
            String result = chatClient.prompt()
                    .user(promptBuilder.toString())
                    .tools(toolsService)  // Register all @Tool methods
                    .call()
                    .content();

            return ToolResponse.success("AI response", result);

        } catch (Exception e) {
            log.error("Failed to process ToolRequest", e);
            return ToolResponse.error("AgentService error", e.getMessage());
        }
    }
}
