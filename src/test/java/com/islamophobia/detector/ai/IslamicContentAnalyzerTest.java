package com.islamophobia.detector.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.islamophobia.detector.config.MockChatModel;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IslamicContentAnalyzerTest {

    @Test
    void parsesStructuredJsonFromMockModel() {
        IslamicContentAnalyzer analyzer = new IslamicContentAnalyzer(new MockChatModel(), new ObjectMapper());

        AnalysisResult result = analyzer.analyzeContent("Example content", Map.of("source", "test"));

        assertFalse(result.isViolation());
        assertEquals("OTHER", result.getCategory());
        assertEquals("0.5", result.getConfidence().stripTrailingZeros().toPlainString());
        assertTrue(result.getExplanation().contains("Mock analysis"));
        assertNotNull(result.getRawResponse());
        assertNotNull(result.getModelUsed());
        assertTrue(result.getQuranReferences().isEmpty());
        assertTrue(result.getHadithReferences().isEmpty());
    }
}

