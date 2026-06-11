package com.islamophobia.detector.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI Service for analyzing content using LLM trained on Quran and Hadith
 */
@Slf4j
@Service
public class IslamicContentAnalyzer {

    private final ChatModel chatModel;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    // System prompt that defines the AI's role and knowledge base
    private static final String SYSTEM_PROMPT = """
        You are an expert Islamic scholar AI assistant specialized in detecting and responding to Islamophobic content.
        Your knowledge is based on the Holy Quran and authentic Hadith collections (Sahih Bukhari, Sahih Muslim, etc.).
        
        Your tasks:
        1. Analyze the provided content for potential Islamophobia or hate speech against Islam/Muslims
        2. Determine if it's a genuine violation or a legitimate criticism/factual statement
        3. If it's a violation, provide:
           - Clear explanation of why it's Islamophobic
           - Counter-argument with evidence from Quran and/or Hadith
           - Specific references (Surah:Ayah for Quran, Book:Hadith number for Hadith)
        4. If it's not a violation, explain why it might be factual or legitimate discourse
        
        Be scholarly, balanced, and cite authentic sources. Always respond in a structured JSON format.
        """;

    public IslamicContentAnalyzer(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    /**
     * Analyze content for Islamophobic violations
     * @param content The text content to analyze
     * @param context Additional context (source, author, etc.)
     * @return AnalysisResult with violation determination and references
     */
    public AnalysisResult analyzeContent(String content, Map<String, String> context) {
        log.info("Analyzing content for Islamophobic violations");

        String userPrompt = buildUserPrompt(content, context);

        String response = chatClient.prompt()
            .system(SYSTEM_PROMPT)
            .user(userPrompt)
            .call()
            .content();

        log.debug("LLM response received");

        return parseAnalysisResult(response);
    }

    /**
     * Analyze multiple content items in batch
     */
    public List<AnalysisResult> analyzeContentBatch(List<String> contents) {
        log.info("Analyzing {} content items in batch", contents.size());
        return contents.stream()
            .map(content -> analyzeContent(content, null))
            .toList();
    }

    private String buildUserPrompt(String content, Map<String, String> context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Please analyze the following content:\n\n");
        prompt.append("CONTENT: \"").append(content).append("\"\n\n");

        if (context != null) {
            prompt.append("Context:\n");
            context.forEach((key, value) ->
                prompt.append("- ").append(key).append(": ").append(value).append("\n"));
        }

        prompt.append("\nProvide your analysis in the following JSON format:\n")
            .append("{\n")
            .append("  \"isViolation\": boolean,\n")
            .append("  \"confidence\": number (0.0-1.0),\n")
            .append("  \"category\": string,\n")
            .append("  \"explanation\": string,\n")
            .append("  \"counterArgument\": string,\n")
            .append("  \"quranReferences\": [\"string\"],\n")
            .append("  \"hadithReferences\": [\"string\"]\n")
            .append("}");

        return prompt.toString();
    }

    private AnalysisResult parseAnalysisResult(String response) {
        log.debug("Parsing LLM response: {}", response);

        String jsonPayload = extractJsonPayload(response);

        try {
            Map<String, Object> payload = objectMapper.readValue(jsonPayload, new TypeReference<>() {});

            return AnalysisResult.builder()
                .isViolation(Boolean.TRUE.equals(payload.get("isViolation")))
                .confidence(parseConfidence(payload.get("confidence")))
                .category(readString(payload.get("category"), "OTHER"))
                .explanation(readString(payload.get("explanation"), "No explanation provided by the model."))
                .counterArgument(readString(payload.get("counterArgument"), ""))
                .quranReferences(parseStringList(payload.get("quranReferences")))
                .hadithReferences(parseStringList(payload.get("hadithReferences")))
                .rawResponse(response)
                .modelUsed(chatModel.toString())
                .tokensUsed(null)
                .build();
        } catch (Exception ex) {
            log.warn("Failed to parse LLM JSON response. Falling back to safe default. Error: {}", ex.getMessage());
            return AnalysisResult.builder()
                .isViolation(false)
                .confidence(BigDecimal.ZERO)
                .category("OTHER")
                .explanation("Unable to parse model response. Raw response stored for review.")
                .counterArgument("")
                .quranReferences(List.of())
                .hadithReferences(List.of())
                .rawResponse(response)
                .modelUsed(chatModel.toString())
                .build();
        }
    }

    private String extractJsonPayload(String response) {
        if (response == null) {
            return "{}";
        }

        String trimmed = response.trim();
        if (trimmed.startsWith("```") && trimmed.endsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed;
    }

    private BigDecimal parseConfidence(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private String readString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private List<String> parseStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                .filter(item -> item != null && !item.toString().isBlank())
                .map(Object::toString)
                .toList();
        }
        return List.of();
    }
}
