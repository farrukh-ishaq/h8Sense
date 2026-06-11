package com.islamophobia.detector.service;

import com.islamophobia.detector.ai.AnalysisResult;
import com.islamophobia.detector.ai.IslamicContentAnalyzer;
import com.islamophobia.detector.model.entity.ContentAnalysis;
import com.islamophobia.detector.model.entity.ContentItem;
import com.islamophobia.detector.model.enums.SourceType;
import com.islamophobia.detector.repository.ContentAnalysisRepository;
import com.islamophobia.detector.repository.ContentItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:analysis-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(ContentAnalysisService.class)
class ContentAnalysisServiceDataJpaTest {

    @Autowired
    private ContentAnalysisService contentAnalysisService;

    @Autowired
    private ContentItemRepository contentItemRepository;

    @Autowired
    private ContentAnalysisRepository contentAnalysisRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private IslamicContentAnalyzer islamicContentAnalyzer;

    @Test
    void processContentPersistsParentItemBeforeAnalysis() {
        when(islamicContentAnalyzer.analyzeContent(any(), anyMap())).thenReturn(AnalysisResult.builder()
            .isViolation(true)
            .confidence(BigDecimal.valueOf(0.91))
            .category("STEREOTYPING")
            .explanation("Detected harmful stereotyping")
            .counterArgument("Counter argument")
            .quranReferences(List.of("49:13"))
            .hadithReferences(List.of("Muslim 2564"))
            .rawResponse("{}")
            .modelUsed("test-model")
            .tokensUsed(42)
            .build());

        ContentItem contentItem = new ContentItem();
        contentItem.setContent("Muslims are all the same");
        contentItem.setTitle("Problematic post");
        contentItem.setSource("https://example.test/post");
        contentItem.setSourceUrl("https://example.test/post");
        contentItem.setAuthor("tester");
        contentItem.setSourceType(SourceType.SOCIAL_MEDIA);
        contentItem.setSourcePlatform("Test Platform");
        contentItem.setDetectedAt(Instant.now());

        ContentAnalysis savedAnalysis = contentAnalysisService.processContent(contentItem);

        assertNotNull(savedAnalysis.getId());
        assertNotNull(savedAnalysis.getContentItem());
        assertNotNull(savedAnalysis.getContentItem().getId());
        assertEquals(1, contentItemRepository.count());
        assertEquals(1, contentAnalysisRepository.count());
        assertEquals(savedAnalysis.getContentItem().getId(), savedAnalysis.getContentItem().getId());
        assertTrue(contentItemRepository.existsById(savedAnalysis.getContentItem().getId()));
    }

    @Test
    void processContentReusesExistingAnalysis() {
        when(islamicContentAnalyzer.analyzeContent(any(), anyMap())).thenReturn(AnalysisResult.builder()
            .isViolation(false)
            .confidence(BigDecimal.valueOf(0.25))
            .category("OTHER")
            .explanation("No violation")
            .counterArgument("")
            .quranReferences(List.of())
            .hadithReferences(List.of())
            .rawResponse("{}")
            .modelUsed("test-model")
            .build());

        ContentItem contentItem = new ContentItem();
        contentItem.setContent("Neutral article about Ramadan");
        contentItem.setTitle("Neutral post");
        contentItem.setSourceType(SourceType.NEWS_ARTICLE);
        contentItem.setSourcePlatform("Reuters");
        contentItem.setDetectedAt(Instant.now());

        ContentAnalysis first = contentAnalysisService.processContent(contentItem);
        ContentAnalysis second = contentAnalysisService.processContent(first.getContentItem());

        assertEquals(first.getId(), second.getId());
        assertEquals(1, contentAnalysisRepository.count());
        verify(islamicContentAnalyzer, times(1)).analyzeContent(any(), anyMap());
    }

    @Test
    void getAnalysisDetailsLoadsReferencesForDetachedUiUsage() {
        when(islamicContentAnalyzer.analyzeContent(any(), anyMap())).thenReturn(AnalysisResult.builder()
            .isViolation(true)
            .confidence(BigDecimal.valueOf(0.88))
            .category("STEREOTYPING")
            .explanation("Detected harmful stereotyping")
            .counterArgument("Counter argument")
            .quranReferences(List.of("49:13"))
            .hadithReferences(List.of("Muslim 2564"))
            .rawResponse("{}")
            .modelUsed("test-model")
            .build());

        ContentItem contentItem = new ContentItem();
        contentItem.setContent("Hostile content about Muslims");
        contentItem.setTitle("Detailed post");
        contentItem.setSourceType(SourceType.SOCIAL_MEDIA);
        contentItem.setSourcePlatform("Test Platform");
        contentItem.setDetectedAt(Instant.now());

        ContentAnalysis savedAnalysis = contentAnalysisService.processContent(contentItem);

        entityManager.flush();
        entityManager.clear();

        ContentAnalysis details = contentAnalysisService.getAnalysisDetails(savedAnalysis.getId())
            .orElseThrow();

        assertEquals("Detailed post", details.getContentItem().getTitle());
        assertEquals(List.of("49:13"), details.getQuranReferences());
        assertEquals(List.of("Muslim 2564"), details.getHadithReferences());
    }
}

