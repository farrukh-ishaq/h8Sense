package com.islamophobia.detector.service;

import com.islamophobia.detector.ai.AnalysisResult;
import com.islamophobia.detector.ai.IslamicContentAnalyzer;
import com.islamophobia.detector.model.dto.FeedbackResponse;
import com.islamophobia.detector.model.entity.*;
import com.islamophobia.detector.model.enums.ViolationCategory;
import com.islamophobia.detector.repository.*;
import org.hibernate.Hibernate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Main service for content analysis and feedback management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentAnalysisService {

    private final IslamicContentAnalyzer aiAnalyzer;
    private final ContentItemRepository contentItemRepository;
    private final ContentAnalysisRepository analysisRepository;
    private final UserFeedbackRepository feedbackRepository;
    private final FeedbackTrackingRepository trackingRepository;

    /**
     * Process new content: analyze with AI and store results
     */
    @Transactional
    public ContentAnalysis processContent(ContentItem contentItem) {
        log.info("Processing content item: {}", contentItem.getId());

        ContentItem savedContent = persistContentItem(contentItem);

        Optional<ContentAnalysis> existingAnalysis = analysisRepository.findByContentItemId(savedContent.getId());
        if (existingAnalysis.isPresent()) {
            log.debug("Analysis already exists for content item {}. Reusing existing record.", savedContent.getId());
            return existingAnalysis.get();
        }

        // Prepare context for AI analysis
        Map<String, String> context = Map.of(
            "source", savedContent.getSourcePlatform() != null ? savedContent.getSourcePlatform() : "Unknown",
            "author", savedContent.getAuthor() != null ? savedContent.getAuthor() : "Unknown"
        );

        // Analyze with AI
        AnalysisResult analysisResult = aiAnalyzer.analyzeContent(
            savedContent.getContent(),
            context
        );

        // Create detailed analysis record
        ContentAnalysis analysis = ContentAnalysis.builder()
            .contentItem(savedContent)
            .isConfirmedViolation(analysisResult.isViolation())
            .category(resolveCategory(analysisResult.getCategory()))
            .violationConfidence(analysisResult.getConfidence())
            .violationExplanation(analysisResult.getExplanation())
            .counterArgument(analysisResult.getCounterArgument())
            .quranReferences(analysisResult.getQuranReferences())
            .hadithReferences(analysisResult.getHadithReferences())
            .llmRawResponse(analysisResult.getRawResponse())
            .modelUsed(analysisResult.getModelUsed())
            .tokensUsed(analysisResult.getTokensUsed())
            .analyzedAt(Instant.now())
            .build();

        if (!contentItemRepository.existsById(savedContent.getId())) {
            throw new IllegalStateException("Content item was not persisted before analysis save: " + savedContent.getId());
        }

        analysis.setContentItem(savedContent);
        ContentAnalysis savedAnalysis = analysisRepository.saveAndFlush(analysis);
        savedContent.setAnalysis(savedAnalysis);
        return savedAnalysis;
    }

    /**
     * Submit user feedback on an analysis
     */
    @Transactional
    public FeedbackResponse submitFeedback(UUID analysisId,
                                           UserFeedback.FeedbackType feedbackType,
                                           String ipAddress,
                                           String deviceFingerprint,
                                           String userAgent,
                                           String comment) {
        ContentAnalysis analysis = analysisRepository.findById(analysisId)
            .orElseThrow(() -> new RuntimeException("Analysis not found: " + analysisId));

        // Check for existing feedback from this IP/device
        Optional<FeedbackTracking> existingTracking = trackingRepository
            .findByIpAddressAndAnalysisId(ipAddress, analysisId);

        if (existingTracking.isPresent()) {
            FeedbackTracking tracking = existingTracking.get();

            // Check if blocked
            if (tracking.isBlocked()) {
                return FeedbackResponse.builder()
                    .success(false)
                    .message("Your IP has been temporarily blocked due to suspicious activity")
                    .isDuplicate(true)
                    .build();
            }

            // Check rate limit (e.g., max 5 feedbacks per IP per analysis)
            if (tracking.getFeedbackCount() >= 5) {
                tracking.setBlocked(true);
                tracking.setBlockReason("Exceeded feedback limit");
                trackingRepository.save(tracking);

                return FeedbackResponse.builder()
                    .success(false)
                    .message("Feedback limit reached for this content")
                    .isDuplicate(true)
                    .build();
            }

            // Increment counter
            trackingRepository.incrementFeedbackCount(tracking.getId(), Instant.now());
        } else {
            // Create new tracking record
            FeedbackTracking newTracking = FeedbackTracking.builder()
                .ipAddress(ipAddress)
                .deviceFingerprint(deviceFingerprint)
                .analysis(analysis)
                .feedbackCount(1)
                .lastFeedbackAt(Instant.now())
                .firstFeedbackAt(Instant.now())
                .build();
            trackingRepository.save(newTracking);
        }

        // Create and save the feedback
        UserFeedback feedback = UserFeedback.builder()
            .analysis(analysis)
            .feedbackType(feedbackType)
            .ipAddress(ipAddress)
            .deviceFingerprint(deviceFingerprint)
            .userAgent(userAgent)
            .comment(comment)
            .isVerified(false)
            .build();

        UserFeedback savedFeedback = feedbackRepository.save(feedback);

        // Calculate totals
        long totalLikes = feedbackRepository.countByAnalysisIdAndFeedbackType(
            analysisId, UserFeedback.FeedbackType.LIKE);
        long totalDislikes = feedbackRepository.countByAnalysisIdAndFeedbackType(
            analysisId, UserFeedback.FeedbackType.DISLIKE);

        return FeedbackResponse.builder()
            .success(true)
            .message("Thank you for your feedback")
            .feedbackId(savedFeedback.getId())
            .totalLikes(totalLikes)
            .totalDislikes(totalDislikes)
            .isDuplicate(false)
            .build();
    }

    /**
     * Get all confirmed violations with pagination
     */
    @Transactional(readOnly = true)
    public Page<ContentAnalysis> getViolations(Pageable pageable) {
        return analysisRepository.findByIsConfirmedViolationTrue(pageable);
    }

    /**
     * Get the most recent analyses, including non-violations, for dashboard fallback views.
     */
    @Transactional(readOnly = true)
    public Page<ContentAnalysis> getRecentAnalyses(Pageable pageable) {
        return analysisRepository.findAllByOrderByAnalyzedAtDesc(pageable);
    }

    /**
     * Get violations by category
     */
    @Transactional(readOnly = true)
    public Page<ContentAnalysis> getViolationsByCategory(String category, Pageable pageable) {
        try {
            ViolationCategory cat = ViolationCategory.valueOf(category.toUpperCase());
            return analysisRepository.findByIsConfirmedViolationTrueAndCategory(cat, pageable);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid violation category requested: {}. Returning empty page.", category);
            return Page.empty(pageable);
        }
    }

    /**
     * Get analysis by content ID
     */
    @Transactional(readOnly = true)
    public Optional<ContentAnalysis> getAnalysisByContentId(UUID contentId) {
        return analysisRepository.findByContentItemId(contentId);
    }

    /**
     * Get a fully initialized analysis for detail rendering in the UI.
     */
    @Transactional(readOnly = true)
    public Optional<ContentAnalysis> getAnalysisDetails(UUID analysisId) {
        return analysisRepository.findWithContentItemById(analysisId)
            .map(analysis -> {
                Hibernate.initialize(analysis.getQuranReferences());
                Hibernate.initialize(analysis.getHadithReferences());
                return analysis;
            });
    }

    /**
     * Get feedback statistics for all analyses
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getFeedbackStats() {
        long likes = feedbackRepository.countByFeedbackType(UserFeedback.FeedbackType.LIKE);
        long dislikes = feedbackRepository.countByFeedbackType(UserFeedback.FeedbackType.DISLIKE);

        return Map.of(
            "likes", likes,
            "dislikes", dislikes
        );
    }

    /**
     * Dashboard-level summary statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getDashboardStats() {
        long totalAnalyses = analysisRepository.count();
        long confirmedViolations = analysisRepository.countByIsConfirmedViolationTrue();
        long pendingContent = contentItemRepository.countByAnalysisIsNull();

        return Map.of(
            "totalAnalyses", totalAnalyses,
            "confirmedViolations", confirmedViolations,
            "nonViolations", Math.max(totalAnalyses - confirmedViolations, 0),
            "pendingContent", pendingContent
        );
    }

    /**
     * Get feedback statistics for a specific analysis
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getFeedbackStats(UUID analysisId) {
        long likes = feedbackRepository.countByAnalysisIdAndFeedbackType(
            analysisId, UserFeedback.FeedbackType.LIKE);
        long dislikes = feedbackRepository.countByAnalysisIdAndFeedbackType(
            analysisId, UserFeedback.FeedbackType.DISLIKE);

        return Map.of(
            "likes", likes,
            "dislikes", dislikes
        );
    }

    private ContentItem persistContentItem(ContentItem contentItem) {
        if (contentItem.getId() != null) {
            return contentItemRepository.findById(contentItem.getId())
                .orElseGet(() -> contentItemRepository.saveAndFlush(contentItem));
        }
        return contentItemRepository.saveAndFlush(contentItem);
    }

    private ViolationCategory resolveCategory(String category) {
        if (category == null || category.isBlank()) {
            return ViolationCategory.OTHER;
        }

        try {
            return ViolationCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown violation category '{}' returned by analyzer. Falling back to OTHER.", category);
            return ViolationCategory.OTHER;
        }
    }
}
