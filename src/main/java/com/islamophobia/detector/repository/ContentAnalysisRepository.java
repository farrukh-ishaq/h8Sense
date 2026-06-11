package com.islamophobia.detector.repository;

import com.islamophobia.detector.model.entity.ContentAnalysis;
import com.islamophobia.detector.model.enums.ViolationCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentAnalysisRepository extends JpaRepository<ContentAnalysis, UUID> {

    @EntityGraph(attributePaths = "contentItem")
    Optional<ContentAnalysis> findWithContentItemById(UUID id);

    @EntityGraph(attributePaths = "contentItem")
    Optional<ContentAnalysis> findByContentItemId(UUID contentItemId);

    @EntityGraph(attributePaths = "contentItem")
    Page<ContentAnalysis> findByIsConfirmedViolation(boolean isViolation, Pageable pageable);

    @EntityGraph(attributePaths = "contentItem")
    Page<ContentAnalysis> findByIsConfirmedViolationTrue(Pageable pageable);

    @EntityGraph(attributePaths = "contentItem")
    Page<ContentAnalysis> findByIsConfirmedViolationTrueAndCategory(ViolationCategory category, Pageable pageable);

    @EntityGraph(attributePaths = "contentItem")
    Page<ContentAnalysis> findAllByOrderByAnalyzedAtDesc(Pageable pageable);

    long countByIsConfirmedViolationTrue();

    @Query("SELECT ca FROM ContentAnalysis ca JOIN FETCH ca.contentItem WHERE ca.isConfirmedViolation = true")
    Page<ContentAnalysis> findViolationsWithContent(Pageable pageable);

    List<ContentAnalysis> findByModelUsed(String modelUsed);
}
