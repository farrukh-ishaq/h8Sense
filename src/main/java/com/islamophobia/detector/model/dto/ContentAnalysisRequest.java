package com.islamophobia.detector.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for content analysis requests
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentAnalysisRequest {
    
    /**
     * The text content to analyze
     */
    private String content;
    
    /**
     * Source type (e.g., SOCIAL_MEDIA, NEWS_ARTICLE, FORUM)
     */
    private String sourceType;
    
    /**
     * Platform name (e.g., Twitter, Facebook, Reddit)
     */
    private String platform;
    
    /**
     * Author/username of the content
     */
    private String author;
    
    /**
     * URL where the content was found
     */
    private String url;
}
