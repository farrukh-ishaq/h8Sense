package com.islamophobia.detector.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity to track unique feedback attempts by IP/device to prevent spam
 */
@Entity
@Table(name = "feedback_tracking", indexes = {
    @Index(name = "idx_tracking_ip", columnList = "ip_address"),
    @Index(name = "idx_tracking_device", columnList = "device_fingerprint"),
    @Index(name = "idx_tracking_analysis", columnList = "analysis_id")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "analysis"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "analysis")
public class FeedbackTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * IP address
     */
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    /**
     * Device fingerprint
     */
    @Column(name = "device_fingerprint", length = 255)
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
    @Column(name = "last_feedback_at", nullable = false)
    private Instant lastFeedbackAt;

    /**
     * First feedback timestamp
     */
    @Column(name = "first_feedback_at", nullable = false)
    private Instant firstFeedbackAt;

    /**
     * Whether this source is blocked due to suspicious activity
     */
    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private boolean isBlocked = false;

    /**
     * Reason for blocking if applicable
     */
    @Column(name = "block_reason", length = 500)
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
