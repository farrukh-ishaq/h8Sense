package com.islamophobia.detector.service;

import com.islamophobia.detector.ai.AnalysisResult;
import com.islamophobia.detector.ai.IslamicContentAnalyzer;
import com.islamophobia.detector.model.dto.FeedbackResponse;
import com.islamophobia.detector.model.entity.*;
import com.islamophobia.detector.model.enums.ViolationCategory;
import com.islamophobia.detector.repository.*;
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

        // Save the content item first
        ContentItem savedContent = contentItemRepository.save(contentItem);

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
            .category(analysisResult.getCategory() != null ? ViolationCategory.valueOf(analysisResult.getCategory()) : ViolationCategory.OTHER)
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

        // Save analysis - the contentItem is already saved, so set it explicitly
        analysis.setContentItem(savedContent);
        return analysisRepository.save(analysis);
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
            trackingRepository.incrementFeedbackCount(tracking.getId());
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
}
