package com.simplecoder.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Minimal configuration to provide a ChatClient bean backed by the auto-configured ChatModel
 * so that ModelToolSelectionStrategy can inject ChatClient.
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        // ChatModel (e.g., OpenAiChatModel) is auto-configured by spring-ai-starter-model-openai
        return ChatClient.create(chatModel);
    }
}
