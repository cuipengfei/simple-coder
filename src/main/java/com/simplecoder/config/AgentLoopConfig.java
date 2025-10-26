package com.simplecoder.config;

import com.simplecoder.service.AgentLoopService;
import com.simplecoder.service.SingleTurnExecutor;
import com.simplecoder.service.ToolsService;
import com.simplecoder.service.TurnResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration wiring full multi-step loop integration.
 * R-11/R-12: Provides SingleTurnExecutor binding the existing ChatClient tool invocation,
 * and AgentLoopService using configured maxSteps.
 * IF-4: Extracts tool metadata from ChatResponse for accurate loop reporting.
 */
@Configuration
@EnableConfigurationProperties(AgentLoopProperties.class)
public class AgentLoopConfig {

    @Bean
    public SingleTurnExecutor singleTurnExecutor(ChatClient chatClient, ToolsService toolsService) {
        return prompt -> {
            var response = chatClient
                    .prompt()
                    .user(prompt)
                    .tools(toolsService)
                    .call()
                    .chatResponse();

            String content = response.getResult().getOutput().getText();
            String toolName = extractToolName(response);

            return new TurnResult(content, toolName);
        };
    }

    /**
     * Extract tool name from ChatResponse.
     * Checks if the assistant message contains tool calls and returns the first tool name.
     *
     * @param response ChatResponse from ChatClient
     * @return tool name if tool was called, null otherwise
     */
    private String extractToolName(ChatResponse response) {
        var result = response.getResult();

        var assistantMessage = result.getOutput();

        var toolCalls = assistantMessage.getToolCalls();
        if (toolCalls.isEmpty()) {
            return null;
        }

        // Return first tool name (most prompts invoke single tool per turn)
        return toolCalls.getFirst().name();
    }

    @Bean
    public AgentLoopService agentLoopService(AgentLoopProperties properties, SingleTurnExecutor singleTurnExecutor) {
        return new AgentLoopService(properties, singleTurnExecutor);
    }
}
