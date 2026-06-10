package com.islamophobia.detector.repository;

import com.islamophobia.detector.model.entity.ContentItem;
import com.islamophobia.detector.model.enums.ViolationCategory;
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
public interface ContentItemRepository extends JpaRepository<ContentItem, UUID> {
    
    Optional<ContentItem> findBySourceTypeAndSourceId(ContentItem.SourceType sourceType, String sourceId);
    
    Page<ContentItem> findByIsViolation(boolean isViolation, Pageable pageable);
    
    Page<ContentItem> findByViolationCategory(ViolationCategory category, Pageable pageable);
    
    @Query("SELECT c FROM ContentItem c WHERE c.collectedAt BETWEEN :startDate AND :endDate")
    Page<ContentItem> findByDateRange(@Param("startDate") Instant startDate, 
                                      @Param("endDate") Instant endDate, 
                                      Pageable pageable);
    
    @Query("SELECT c FROM ContentItem c WHERE c.platform = :platform ORDER BY c.publishedAt DESC")
    Page<ContentItem> findByPlatform(@Param("platform") String platform, Pageable pageable);
    
    List<ContentItem> findByIsViolationAndCollectedAtBefore(boolean isViolation, Instant cutoffDate);
}
