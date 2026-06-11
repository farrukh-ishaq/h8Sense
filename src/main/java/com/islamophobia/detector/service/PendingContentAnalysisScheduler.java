package com.islamophobia.detector.service;

import com.islamophobia.detector.model.entity.ContentItem;
import com.islamophobia.detector.repository.ContentItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Backfills analyses for content items that were saved before the schema was repaired.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingContentAnalysisScheduler {

    private final ContentItemRepository contentItemRepository;
    private final ContentAnalysisService contentAnalysisService;

    private final AtomicBoolean backfillInProgress = new AtomicBoolean(false);

    @Value("${app.content.backfill-enabled:true}")
    private boolean backfillEnabled;

    @Value("${app.content.backfill-batch-size:25}")
    private int backfillBatchSize;

    @Scheduled(
        initialDelayString = "${app.content.backfill-initial-delay-ms:10000}",
        fixedDelayString = "${app.content.backfill-fixed-delay-ms:60000}"
    )
    public void backfillPendingContent() {
        if (!backfillEnabled) {
            return;
        }

        if (!backfillInProgress.compareAndSet(false, true)) {
            log.debug("Skipping pending-content backfill because a previous batch is still running.");
            return;
        }

        try {
            List<ContentItem> pendingItems = contentItemRepository
                .findByAnalysisIsNull(PageRequest.of(0, backfillBatchSize, Sort.by("detectedAt").ascending()))
                .getContent();

            if (pendingItems.isEmpty()) {
                return;
            }

            log.info("Backfilling AI analysis for {} pending content items.", pendingItems.size());
            for (ContentItem pendingItem : pendingItems) {
                try {
                    contentAnalysisService.processContent(pendingItem);
                } catch (Exception ex) {
                    log.error("Failed to backfill analysis for content {}: {}", pendingItem.getId(), ex.getMessage());
                }
            }
        } finally {
            backfillInProgress.set(false);
        }
    }
}

