package com.islamophobia.detector.repository;

import com.islamophobia.detector.model.entity.UserFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserFeedbackRepository extends JpaRepository<UserFeedback, UUID> {
    
    Page<UserFeedback> findByAnalysisId(UUID analysisId, Pageable pageable);
    
    List<UserFeedback> findByIpAddress(String ipAddress);
    
    List<UserFeedback> findByDeviceFingerprint(String deviceFingerprint);
    
    @Query("SELECT uf FROM UserFeedback uf WHERE uf.analysis.id = :analysisId AND uf.ipAddress = :ipAddress")
    List<UserFeedback> findByAnalysisIdAndIpAddress(@Param("analysisId") UUID analysisId, 
                                                     @Param("ipAddress") String ipAddress);
    
    @Query("SELECT COUNT(uf) FROM UserFeedback uf WHERE uf.analysis.id = :analysisId AND uf.feedbackType = :type")
    long countByAnalysisIdAndFeedbackType(@Param("analysisId") UUID analysisId, 
                                          @Param("type") UserFeedback.FeedbackType type);
    
    Page<UserFeedback> findByCreatedAtBetween(Instant startDate, Instant endDate, Pageable pageable);
}
