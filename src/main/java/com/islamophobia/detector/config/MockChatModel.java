package com.islamophobia.detector.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Mock ChatModel for development/testing when no API key is available.
 * Returns simulated responses without calling external APIs.
 */
@Slf4j
public class MockChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        log.debug("MockChatModel called with prompt: {}", prompt.getContents());
        
        // Return a mock response indicating no violation detected
        // This allows the app to run without API keys for testing UI and data flow
        String mockResponse = """
            {
                "isViolation": false,
                "confidence": 0.5,
                "category": "NONE",
                "explanation": "Mock analysis - AI not configured. Set OPENAI_API_KEY or use Ollama for real analysis.",
                "counterArgument": "No counter-argument generated - mock mode active.",
                "quranReferences": [],
                "hadithReferences": []
            }
            """;
        
        AssistantMessage message = new AssistantMessage(mockResponse);
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        log.warn("Streaming not fully supported in MockChatModel, using call() instead");
        return Flux.just(call(prompt));
    }
}
