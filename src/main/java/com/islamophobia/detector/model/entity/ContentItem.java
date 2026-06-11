package com.islamophobia.detector.model.entity;

import com.islamophobia.detector.model.enums.SourceType;
import com.islamophobia.detector.model.enums.ViolationCategory;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents content that needs to be analyzed or has been analyzed
 */
@Entity
@Table(name = "content_item", indexes = {
    @Index(name = "idx_content_source_platform", columnList = "sourcePlatform"),
    @Index(name = "idx_content_timestamp", columnList = "createdAt"),
    @Index(name = "idx_content_type", columnList = "sourceType")
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
     * The actual text content to analyze
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Title of the content (for articles, blog posts, etc.)
     */
    @Column(length = 500)
    private String title;

    /**
     * Where the content came from (Twitter, Facebook, etc.)
     */
    @Column(length = 100)
    private String sourcePlatform;

    /**
     * Source URL or identifier for the content
     */
    @Column(length = 500)
    private String source;

    /**
     * URL or identifier for the content
     */
    @Column(length = 500)
    private String sourceUrl;

    /**
     * Type of source (SOCIAL_MEDIA, NEWS_ARTICLE, FORUM, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SourceType sourceType;

    /**
     * Author/username of the content
     */
    @Column(length = 255)
    private String author;

    /**
     * Initial category prediction (optional, can be determined by LLM)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private ViolationCategory predictedCategory;

    /**
     * Additional metadata (JSON stored as string)
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "content_metadata", joinColumns = @JoinColumn(name = "content_item_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", length = 1000)
    private Map<String, String> metadata;

    /**
     * When the content was created/posted
     */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * When the content was detected by our system
     */
    @Column(nullable = false)
    private Instant detectedAt;

    /**
     * One-to-one relationship with analysis
     */
    @OneToOne(mappedBy = "contentItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ContentAnalysis analysis;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (detectedAt == null) {
            detectedAt = Instant.now();
        }
    }
}
