package com.islamophobia.detector.model.entity;

import com.islamophobia.detector.model.enums.ViolationCategory;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SourceType sourceType;
    
    @Column(nullable = false, length = 255)
    private String sourceId;
    
    @Column(nullable = false, length = 100)
    private String platform;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(length = 500)
    private String title;
    
    @Column(length = 255)
    private String author;
    
    @Column(length = 2048)
    private String url;
    
    @Column(nullable = false)
    private Instant publishedAt;
    
    @Column(nullable = false)
    private Instant collectedAt;
    
    @Column(nullable = false)
    private boolean isViolation = false;
    
    @Column(columnDefinition = "DOUBLE PRECISION")
    private Double violationConfidence;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private ViolationCategory violationCategory;
    
    @OneToOne(mappedBy = "contentItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ContentAnalysis analysis;
    
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
