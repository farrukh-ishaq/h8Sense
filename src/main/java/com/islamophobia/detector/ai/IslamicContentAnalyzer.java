package com.islamophobia.detector.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

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
    
    public IslamicContentAnalyzer(ChatModel chatModel) {
        this.chatModel = chatModel;
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
        // In production, use proper JSON parsing with Jackson
        // This is a simplified implementation
        log.debug("Parsing LLM response: {}", response);
        
        AnalysisResult result = new AnalysisResult();
        result.setRawResponse(response);
        
        // TODO: Implement proper JSON parsing
        // For now, return a placeholder
        result.setViolation(false);
        result.setExplanation("Analysis pending proper JSON parser implementation");
        
        return result;
    }
}
