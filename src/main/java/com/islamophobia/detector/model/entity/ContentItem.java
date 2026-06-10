package com.islamophobia.detector.model.entity;

import com.islamophobia.detector.model.enums.ViolationCategory;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a piece of content (post, article, comment) collected from various sources
 */
@Entity
@Table(name = "content_items", indexes = {
    @Index(name = "idx_content_source", columnList = "sourceType,sourceId"),
    @Index(name = "idx_content_timestamp", columnList = "collectedAt"),
    @Index(name = "idx_content_violation", columnList = "isViolation")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Original source type: NEWS, SOCIAL_MEDIA, FORUM, BLOG, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SourceType sourceType;
    
    /**
     * Unique identifier from the source platform
     */
    @Column(nullable = false, length = 255)
    private String sourceId;
    
    /**
     * Platform name (Twitter, Facebook, CNN, BBC, etc.)
     */
    @Column(nullable = false, length = 100)
    private String platform;
    
    /**
     * Full content text
     */
    @Column(columnDefinition = "TEXT")
    private String content;
    
    /**
     * Title if available
     */
    @Column(length = 500)
    private String title;
    
    /**
     * Author/username who posted the content
     */
    @Column(length = 255)
    private String author;
    
    /**
     * URL to the original content
     */
    @Column(length = 2048)
    private String url;
    
    /**
     * When the content was originally published/posted
     */
    @Column(nullable = false)
    private Instant publishedAt;
    
    /**
     * When we collected this content
     */
    @Column(nullable = false)
    private Instant collectedAt;
    
    /**
     * Whether this content is flagged as a potential violation
     */
    @Column(nullable = false)
    private boolean isViolation = false;
    
    /**
     * Confidence score of violation detection (0.0 to 1.0)
     */
    @Column
    private Double violationConfidence;
    
    /**
     * Category of hate speech detected
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private ViolationCategory violationCategory;
    
    /**
     * Related analysis record
     */
    @OneToOne(mappedBy = "contentItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ContentAnalysis analysis;
    
    /**
     * Engagement metrics
     */
    @Column
    private Integer likes;
    
    @Column
    private Integer shares;
    
    @Column
    private Integer comments;
    
    @PrePersist
    protected void onCreate() {
        if (collectedAt == null) {
            collectedAt = Instant.now();
        }
    }
    
    public enum SourceType {
        NEWS, SOCIAL_MEDIA, FORUM, BLOG, VIDEO, PODCAST, OTHER
    }
}
