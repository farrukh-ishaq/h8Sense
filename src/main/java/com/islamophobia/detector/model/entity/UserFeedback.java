package com.islamophobia.detector.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    @Index(name = "idx_feedback_ip", columnList = "ip_address"),
    @Index(name = "idx_feedback_device", columnList = "device_fingerprint"),
    @Index(name = "idx_feedback_timestamp", columnList = "created_at")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "analysis"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "analysis")
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
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
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Device/browser fingerprint for duplicate detection
     */
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    /**
     * User agent string
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Optional authenticated user ID if logged in
     */
    @Column(name = "user_id", length = 100)
    private String userId;

    /**
     * Optional comment from the user
     */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /**
     * When the feedback was created
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Whether this feedback has been verified/validated
     */
    @Column(name = "is_verified", nullable = false)
    @Builder.Default
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
