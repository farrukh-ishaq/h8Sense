package com.islamophobia.detector.repository;

import com.islamophobia.detector.model.entity.ContentAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentAnalysisRepository extends JpaRepository<ContentAnalysis, UUID> {
    
    Optional<ContentAnalysis> findByContentItemId(UUID contentItemId);
    
    Page<ContentAnalysis> findByIsConfirmedViolation(boolean isViolation, Pageable pageable);
    
    @Query("SELECT ca FROM ContentAnalysis ca JOIN FETCH ca.contentItem WHERE ca.isConfirmedViolation = true")
    Page<ContentAnalysis> findViolationsWithContent(Pageable pageable);
    
    List<ContentAnalysis> findByModelUsed(String modelUsed);
}
