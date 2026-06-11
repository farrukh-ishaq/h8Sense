package com.islamophobia.detector.repository;

import com.islamophobia.detector.model.entity.FeedbackTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedbackTrackingRepository extends JpaRepository<FeedbackTracking, UUID> {

    @Query("SELECT ft FROM FeedbackTracking ft WHERE ft.ipAddress = :ipAddress AND ft.analysis.id = :analysisId")
    Optional<FeedbackTracking> findByIpAddressAndAnalysisId(@Param("ipAddress") String ipAddress,
                                                            @Param("analysisId") UUID analysisId);

    @Query("SELECT ft FROM FeedbackTracking ft WHERE ft.deviceFingerprint = :fingerprint AND ft.analysis.id = :analysisId")
    Optional<FeedbackTracking> findByDeviceFingerprintAndAnalysisId(@Param("fingerprint") String fingerprint,
                                                                     @Param("analysisId") UUID analysisId);

    List<FeedbackTracking> findByIsBlocked(boolean isBlocked);

    @Modifying
    @Query("UPDATE FeedbackTracking ft SET ft.feedbackCount = ft.feedbackCount + 1, ft.lastFeedbackAt = :timestamp WHERE ft.id = :id")
    void incrementFeedbackCount(@Param("id") UUID id, @Param("timestamp") Instant timestamp);
}
