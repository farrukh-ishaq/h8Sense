package com.islamophobia.detector.controller;

import com.islamophobia.detector.model.dto.FeedbackRequest;
import com.islamophobia.detector.model.dto.FeedbackResponse;
import com.islamophobia.detector.model.entity.ContentAnalysis;
import com.islamophobia.detector.model.entity.UserFeedback;
import com.islamophobia.detector.service.ContentAnalysisService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST API Controller for content analysis and feedback
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContentAnalysisController {
    
    private final ContentAnalysisService analysisService;
    
    /**
     * Get all confirmed violations (feed generation)
     */
    @GetMapping("/violations")
    public ResponseEntity<Page<ContentAnalysis>> getViolations(
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Fetching violations page {}", pageable.getPageNumber());
        return ResponseEntity.ok(analysisService.getViolations(pageable));
    }
    
    /**
     * Get specific analysis by content ID
     */
    @GetMapping("/content/{contentId}/analysis")
    public ResponseEntity<ContentAnalysis> getAnalysisByContentId(
            @PathVariable UUID contentId) {
        log.info("Fetching analysis for content {}", contentId);
        
        Optional<ContentAnalysis> analysis = analysisService.getAnalysisByContentId(contentId);
        return analysis.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    /**
     * Get feedback statistics for an analysis
     */
    @GetMapping("/analysis/{analysisId}/feedback")
    public ResponseEntity<Map<String, Long>> getFeedbackStats(
            @PathVariable UUID analysisId) {
        log.info("Fetching feedback stats for analysis {}", analysisId);
        return ResponseEntity.ok(analysisService.getFeedbackStats(analysisId));
    }
    
    /**
     * Submit feedback (like/dislike) on an analysis
     */
    @PostMapping("/analysis/{analysisId}/feedback")
    public ResponseEntity<FeedbackResponse> submitFeedback(
            @PathVariable UUID analysisId,
            @RequestBody FeedbackRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Received feedback request for analysis {}", analysisId);
        
        // Extract IP address
        String ipAddress = getClientIpAddress(httpRequest);
        
        // Extract device fingerprint from headers (simplified - in production use proper fingerprinting)
        String deviceFingerprint = httpRequest.getHeader("User-Agent") + 
                                   "-" + httpRequest.getHeader("Accept-Language");
        
        // Extract user agent
        String userAgent = httpRequest.getHeader("User-Agent");
        
        // Parse feedback type
        UserFeedback.FeedbackType feedbackType;
        try {
            feedbackType = UserFeedback.FeedbackType.valueOf(request.getFeedbackType().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                FeedbackResponse.builder()
                    .success(false)
                    .message("Invalid feedback type. Must be LIKE or DISLIKE")
                    .build()
            );
        }
        
        // Submit feedback
        FeedbackResponse response = analysisService.submitFeedback(
            ipAddress,
            deviceFingerprint,
            userAgent,
            analysisId,
            feedbackType,
            request.getComment()
        );
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(429).body(response); // Too Many Requests
        }
    }
    
    /**
     * Helper method to extract client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] HEADERS_TO_TRY = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : HEADERS_TO_TRY) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
}
