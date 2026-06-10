package com.islamophobia.detector.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

/**
 * Configuration for LLM providers (OpenAI and Ollama)
 * Automatically selects the appropriate ChatModel based on configuration
 */
@Configuration
public class LlmConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String openAiModel;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat.options.model:llama3.1:8b-instruct-q4_0}")
    private String ollamaModel;

    @Value("${llm.provider:openai}")
    private String llmProvider;

    /**
     * Primary ChatModel bean - switches between OpenAI and Ollama based on configuration
     */
    @Bean
    @Primary
    public ChatModel chatModel() {
        if ("ollama".equalsIgnoreCase(llmProvider)) {
            log.info("Configuring Ollama ChatModel with model: {}", ollamaModel);
            OllamaApi ollamaApi = new OllamaApi(ollamaBaseUrl);
            return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaOptions.builder()
                    .model(ollamaModel)
                    .temperature(0.3)
                    .build())
                .build();
        } else {
            log.info("Configuring OpenAI ChatModel with model: {}", openAiModel);
            OpenAiApi openAiApi = new OpenAiApi(openAiApiKey);
            return new OpenAiChatModel(openAiApi, OpenAiChatOptions.builder()
                .model(openAiModel)
                .temperature(0.3)
                .build());
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LlmConfig.class);
}
