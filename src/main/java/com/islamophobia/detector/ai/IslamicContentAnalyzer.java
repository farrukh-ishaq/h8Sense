package com.islamophobia.detector.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    @Value("${spring.ai.ollama.chat.options.model:}")
    private String configuredModelName = "";

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

    IslamicContentAnalyzer overrideConfiguredModelName(String configuredModelName) {
        this.configuredModelName = configuredModelName == null ? "" : configuredModelName.trim();
        return this;
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

        String response;
        try {
            response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();
        } catch (Exception ex) {
            log.warn("LLM call failed. Falling back to safe default analysis. Error: {}", ex.getMessage());
            return buildFallbackResult(ex);
        }

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
                .modelUsed(resolveModelName())
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
                .modelUsed(resolveModelName())
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

    private AnalysisResult buildFallbackResult(Exception ex) {
        String errorMessage = ex == null || ex.getMessage() == null
            ? "Unknown AI provider error"
            : ex.getMessage().trim();

        String explanation = "AI analysis is currently unavailable, so the item was stored with a safe default result. " +
            buildOperatorHint(errorMessage);

        return AnalysisResult.builder()
            .isViolation(false)
            .confidence(BigDecimal.ZERO)
            .category("OTHER")
            .explanation(explanation)
            .counterArgument("")
            .quranReferences(List.of())
            .hadithReferences(List.of())
            .rawResponse(errorMessage)
            .modelUsed(resolveModelName())
            .tokensUsed(null)
            .build();
    }

    private String resolveModelName() {
        String resolved = extractModelName(chatModel);
        if (hasText(resolved)) {
            return resolved.trim();
        }
        if (hasText(configuredModelName)) {
            return configuredModelName;
        }

        String simpleName = chatModel.getClass().getSimpleName();
        if (hasText(simpleName)) {
            return simpleName;
        }

        String fallback = chatModel.toString();
        return hasText(fallback) ? fallback : "Unknown model";
    }

    private String extractModelName(Object source) {
        if (source == null) {
            return null;
        }

        String directModel = invokeStringGetter(source, "getModel");
        if (hasText(directModel)) {
            return directModel;
        }

        Object options = invokeGetter(source, "getDefaultOptions");
        if (options == null) {
            options = invokeGetter(source, "getOptions");
        }
        if (options == null) {
            options = readField(source, "defaultOptions");
        }
        if (options == null) {
            options = readField(source, "options");
        }

        if (options != null) {
            String optionModel = invokeStringGetter(options, "getModel");
            if (hasText(optionModel)) {
                return optionModel;
            }

            Object fieldModel = readField(options, "model");
            if (fieldModel != null && hasText(fieldModel.toString())) {
                return fieldModel.toString().trim();
            }
        }

        return null;
    }

    private Object invokeGetter(Object source, String methodName) {
        try {
            Method method = source.getClass().getMethod(methodName);
            return method.invoke(source);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String invokeStringGetter(Object source, String methodName) {
        Object value = invokeGetter(source, methodName);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Object readField(Object source, String fieldName) {
        Class<?> type = source.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(source);
            } catch (Exception ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String buildOperatorHint(String errorMessage) {
        String normalizedError = errorMessage == null ? "" : errorMessage.toLowerCase();

        if (normalizedError.contains("model") && normalizedError.contains("not found")) {
            return "The configured Ollama model was not found. Pull the model with `ollama pull <model>` or update `OLLAMA_MODEL` to a model that exists in your Ollama instance.";
        }

        if (normalizedError.contains("connection refused") || normalizedError.contains("failed to connect") || normalizedError.contains("connect timed out")) {
            return "The configured AI provider could not be reached. Verify `OLLAMA_HOST`/network connectivity or switch to `LLM_PROVIDER=none` while onboarding.";
        }

        return "Check the application logs and validate the configured LLM provider, host, and model.";
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
