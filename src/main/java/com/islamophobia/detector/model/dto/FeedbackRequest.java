package com.islamophobia.detector.model.dto;

import lombok.*;
import java.util.UUID;

/**
 * Request DTO for submitting feedback
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackRequest {
    
    private UUID analysisId;
    private String feedbackType; // "LIKE" or "DISLIKE"
    private String comment;
}
