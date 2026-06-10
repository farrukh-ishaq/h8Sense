package com.islamophobia.detector.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Detailed analysis of content by the LLM with Quran and Hadith references
 */
@Entity
@Table(name = "content_analysis", indexes = {
    @Index(name = "idx_analysis_content", columnList = "contentItem_id"),
    @Index(name = "idx_analysis_timestamp", columnList = "analyzedAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Reference to the analyzed content
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_item_id", nullable = false, unique = true)
    private ContentItem contentItem;
    
    /**
     * Whether this is actually a violation (LLM determination)
     */
    @Column(nullable = false)
    private boolean isConfirmedViolation;
    
    /**
     * Detailed explanation of why it is/isn't a violation
     */
    @Column(columnDefinition = "TEXT")
    private String violationExplanation;
    
    /**
     * Counter-argument or factual correction
     */
    @Column(columnDefinition = "TEXT")
    private String counterArgument;
    
    /**
     * References to Quran verses
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quran_references", 
                     joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(length = 255)
    @Builder.Default
    private List<String> quranReferences = new ArrayList<>();
    
    /**
     * References to Hadith
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hadith_references", 
                     joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(length = 500)
    @Builder.Default
    private List<String> hadithReferences = new ArrayList<>();
    
    /**
     * Full LLM response/raw analysis
     */
    @Column(columnDefinition = "TEXT")
    private String llmRawResponse;
    
    /**
     * Model used for analysis
     */
    @Column(length = 100)
    private String modelUsed;
    
    /**
     * Tokens consumed in the analysis
     */
    @Column
    private Integer tokensUsed;
    
    /**
     * When the analysis was performed
     */
    @Column(nullable = false)
    private Instant analyzedAt;
    
    /**
     * User feedback on the analysis quality
     */
    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserFeedback> userFeedbacks = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (analyzedAt == null) {
            analyzedAt = Instant.now();
        }
        if (quranReferences == null) {
            quranReferences = new ArrayList<>();
        }
        if (hadithReferences == null) {
            hadithReferences = new ArrayList<>();
        }
    }
}
