package com.islamophobia.detector.model.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for feedback submission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResponse {
    
    private boolean success;
    private String message;
    private UUID feedbackId;
    private long totalLikes;
    private long totalDislikes;
    private boolean isDuplicate;
}
