package com.islamophobia.detector.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islamophobia.detector.config.MockChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IslamicContentAnalyzerTest {

    @Test
    void parsesStructuredJsonFromMockModel() {
        IslamicContentAnalyzer analyzer = new IslamicContentAnalyzer(new MockChatModel(), new ObjectMapper());

        AnalysisResult result = analyzer.analyzeContent("Example content", Map.of("source", "test"));

        assertFalse(result.isViolation());
        assertEquals("OTHER", result.getCategory());
        assertEquals("0.5", result.getConfidence().stripTrailingZeros().toPlainString());
        assertTrue(result.getExplanation().contains("Mock analysis"));
        assertNotNull(result.getRawResponse());
        assertNotNull(result.getModelUsed());
        assertTrue(result.getQuranReferences().isEmpty());
        assertTrue(result.getHadithReferences().isEmpty());
    }

    @Test
    void fallsBackToSafeDefaultWhenChatModelFails() {
        ChatModel failingModel = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                throw new RuntimeException("model 'llama3.1' not found");
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.error(new RuntimeException("streaming not supported"));
            }

            @Override
            public String toString() {
                return "FailingChatModel";
            }
        };

        IslamicContentAnalyzer analyzer = new IslamicContentAnalyzer(failingModel, new ObjectMapper());

        AnalysisResult result = analyzer.analyzeContent("Example content", Map.of("source", "test"));

        assertFalse(result.isViolation());
        assertEquals("OTHER", result.getCategory());
        assertEquals("0", result.getConfidence().stripTrailingZeros().toPlainString());
        assertTrue(result.getExplanation().contains("AI analysis is currently unavailable"));
        assertTrue(result.getExplanation().contains("Ollama model"));
        assertEquals("FailingChatModel", result.getModelUsed());
        assertTrue(result.getQuranReferences().isEmpty());
        assertTrue(result.getHadithReferences().isEmpty());
    }
}

