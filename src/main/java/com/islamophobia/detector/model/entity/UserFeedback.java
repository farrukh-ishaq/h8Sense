package com.islamophobia.detector.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * User feedback (like/dislike) on content analysis
 * Tracks feedback by IP, device fingerprint, or authenticated user
 */
@Entity
@Table(name = "user_feedback", indexes = {
    @Index(name = "idx_feedback_analysis", columnList = "analysis_id"),
    @Index(name = "idx_feedback_ip", columnList = "ipAddress"),
    @Index(name = "idx_feedback_device", columnList = "deviceFingerprint"),
    @Index(name = "idx_feedback_timestamp", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeedback {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Reference to the analysis being rated
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private ContentAnalysis analysis;
    
    /**
     * Feedback type: LIKE or DISLIKE
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeedbackType feedbackType;
    
    /**
     * IP address of the user providing feedback
     */
    @Column(length = 45)
    private String ipAddress;
    
    /**
     * Device/browser fingerprint for duplicate detection
     */
    @Column(length = 255)
    private String deviceFingerprint;
    
    /**
     * User agent string
     */
    @Column(length = 500)
    private String userAgent;
    
    /**
     * Optional authenticated user ID if logged in
     */
    @Column(length = 100)
    private String userId;
    
    /**
     * Optional comment from the user
     */
    @Column(columnDefinition = "TEXT")
    private String comment;
    
    /**
     * When the feedback was created
     */
    @Column(nullable = false)
    private Instant createdAt;
    
    /**
     * Whether this feedback has been verified/validated
     */
    @Column(nullable = false)
    private boolean isVerified = false;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
    
    public enum FeedbackType {
        LIKE, DISLIKE
    }
}
