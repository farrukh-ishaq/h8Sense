package com.islamophobia.detector.ai;

import lombok.*;
import java.util.List;

/**
 * Result of AI analysis of content
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {
    
    /**
     * Whether the content is determined to be a violation
     */
    private boolean isViolation;
    
    /**
     * Confidence score (0.0 to 1.0)
     */
    private Double confidence;
    
    /**
     * Category of violation
     */
    private String category;
    
    /**
     * Detailed explanation
     */
    private String explanation;
    
    /**
     * Counter-argument or factual response
     */
    private String counterArgument;
    
    /**
     * References to Quran verses (format: "SurahName:AyahNumber")
     */
    @Builder.Default
    private List<String> quranReferences = new java.util.ArrayList<>();
    
    /**
     * References to Hadith (format: "Collection:Book:HadithNumber")
     */
    @Builder.Default
    private List<String> hadithReferences = new java.util.ArrayList<>();
    
    /**
     * Raw LLM response for debugging/auditing
     */
    private String rawResponse;
    
    /**
     * Model used for analysis
     */
    private String modelUsed;
    
    /**
     * Tokens consumed in the analysis
     */
    private Integer tokensUsed;
}
