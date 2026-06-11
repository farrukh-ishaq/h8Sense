package com.islamophobia.detector.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.islamophobia.detector.model.enums.ViolationCategory;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Detailed analysis of content by the LLM with Quran and Hadith references
 */
@Entity
@Table(name = "content_analysis", indexes = {
    @Index(name = "idx_analysis_content", columnList = "content_item_id"),
    @Index(name = "idx_analysis_timestamp", columnList = "analyzed_at")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "userFeedbacks"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"contentItem", "userFeedbacks"})
public class ContentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Reference to the analyzed content
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_item_id", nullable = false, unique = true,
        foreignKey = @ForeignKey(name = "fk_content_analysis_content_item"))
    private ContentItem contentItem;

    /**
     * Whether this is actually a violation (LLM determination)
     */
    @Column(nullable = false)
    private boolean isConfirmedViolation;

    /**
     * Category of the violation
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private ViolationCategory category;

    /**
     * Confidence score (0.0 to 1.0)
     */
    @Column(name = "violation_confidence", nullable = false, columnDefinition = "DECIMAL(4,3)")
    private BigDecimal violationConfidence;

    /**
     * Detailed explanation of why it is/isn't a violation
     */
    @Column(name = "violation_explanation", columnDefinition = "TEXT")
    private String violationExplanation;

    /**
     * Counter-argument or factual correction
     */
    @Column(name = "counter_argument", columnDefinition = "TEXT")
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
    @Column(name = "llm_raw_response", columnDefinition = "TEXT")
    private String llmRawResponse;

    /**
     * Model used for analysis
     */
    @Column(name = "model_used", length = 100)
    private String modelUsed;

    /**
     * Tokens consumed in the analysis
     */
    @Column(name = "tokens_used")
    private Integer tokensUsed;

    /**
     * When the analysis was performed
     */
    @Column(name = "analyzed_at", nullable = false)
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
        if (violationConfidence == null) {
            violationConfidence = BigDecimal.ZERO;
        }
    }
}
