package com.islamophobia.detector.model.entity;

import com.islamophobia.detector.model.enums.ViolationCategory;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents content that needs to be analyzed or has been analyzed
 */
@Entity
@Table(name = "content_item", indexes = {
    @Index(name = "idx_content_source", columnList = "sourcePlatform"),
    @Index(name = "idx_content_timestamp", columnList = "createdAt")
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
     * Where the content came from (Twitter, Facebook, etc.)
     */
    @Column(length = 100)
    private String sourcePlatform;

    /**
     * URL or identifier for the content
     */
    @Column(length = 500)
    private String sourceUrl;

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
     * When the content was created/posted
     */
    @Column(nullable = false)
    private Instant createdAt;

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
    }
}
