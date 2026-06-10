package com.islamophobia.detector.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "content_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_item_id", nullable = false, unique = true)
    private ContentItem contentItem;
    
    @Column(nullable = false)
    private boolean isConfirmedViolation;
    
    @Column(columnDefinition = "TEXT")
    private String violationExplanation;
    
    @Column(columnDefinition = "TEXT")
    private String counterArgument;
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "quran_references", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(length = 255)
    @Builder.Default
    private List<String> quranReferences = new ArrayList<>();
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hadith_references", joinColumns = @JoinColumn(name = "analysis_id"))
    @Column(length = 500)
    @Builder.Default
    private List<String> hadithReferences = new ArrayList<>();
    
    @Column(columnDefinition = "TEXT")
    private String llmRawResponse;
    
    @Column(length = 100)
    private String modelUsed;
    
    @Column
    private Integer tokensUsed;
    
    @Column(nullable = false)
    private Instant analyzedAt;
    
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
