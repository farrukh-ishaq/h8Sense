package com.islamophobia.detector.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track unique feedback attempts by IP/device to prevent spam
 */
@Entity
@Table(name = "feedback_tracking", indexes = {
    @Index(name = "idx_tracking_ip", columnList = "ipAddress"),
    @Index(name = "idx_tracking_device", columnList = "deviceFingerprint"),
    @Index(name = "idx_tracking_analysis", columnList = "analysis_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackTracking {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * IP address
     */
    @Column(nullable = false, length = 45)
    private String ipAddress;
    
    /**
     * Device fingerprint
     */
    @Column(length = 255)
    private String deviceFingerprint;
    
    /**
     * Reference to the analysis
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private ContentAnalysis analysis;
    
    /**
     * Count of feedbacks from this source for this analysis
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer feedbackCount = 0;
    
    /**
     * Last feedback timestamp
     */
    @Column(nullable = false)
    private Instant lastFeedbackAt;
    
    /**
     * First feedback timestamp
     */
    @Column(nullable = false)
    private Instant firstFeedbackAt;
    
    /**
     * Whether this source is blocked due to suspicious activity
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean isBlocked = false;
    
    /**
     * Reason for blocking if applicable
     */
    @Column(length = 500)
    private String blockReason;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (lastFeedbackAt == null) {
            lastFeedbackAt = now;
        }
        if (firstFeedbackAt == null) {
            firstFeedbackAt = now;
        }
        if (feedbackCount == null) {
            feedbackCount = 0;
        }
    }
}
