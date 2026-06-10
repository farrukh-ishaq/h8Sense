package com.islamophobia.detector.model.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Content Analysis with all related information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentAnalysisDTO {
    
    private UUID id;
    private UUID contentItemId;
    
    // Content details
    private String sourceType;
    private String platform;
    private String author;
    private String title;
    private String content;
    private String url;
    private Instant publishedAt;
    
    // Violation metadata
    private boolean isViolation;
    private boolean isConfirmedViolation;
    private Double violationConfidence;
    private String violationCategory;
    private String violationExplanation;
    private Instant detectedAt;
    
    // Response/Counter-argument
    private String counterArgument;
    private List<String> quranReferences;
    private List<String> hadithReferences;
    
    // User feedback stats
    private long likeCount;
    private long dislikeCount;
    
    // AI analysis info
    private String modelUsed;
    private Instant analyzedAt;
}
