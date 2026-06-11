package com.islamophobia.detector.init;

import com.islamophobia.detector.model.entity.ContentAnalysis;
import com.islamophobia.detector.model.entity.ContentItem;
import com.islamophobia.detector.model.enums.ViolationCategory;
import com.islamophobia.detector.model.enums.SourceType;
import com.islamophobia.detector.repository.ContentAnalysisRepository;
import com.islamophobia.detector.repository.ContentItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ContentAnalysisRepository analysisRepository;
    private final ContentItemRepository itemRepository;

    public DataInitializer(ContentAnalysisRepository analysisRepository, 
                          ContentItemRepository itemRepository) {
        this.analysisRepository = analysisRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (analysisRepository.count() == 0) {
            System.out.println("🌟 Initializing sample data for Islamophobia Detector...");
            loadSampleData();
            System.out.println("✅ Sample data loaded successfully!");
        } else {
            System.out.println("ℹ️  Database already contains data. Skipping initialization.");
        }
    }

    private void loadSampleData() {
        // Sample Case 1: Terrorism Association (mapped to STEREOTYPING)
        ContentItem item1 = createContentItem(
            "All Muslims support terrorism and want to destroy our way of life.",
            "Twitter Simulation",
            "Anonymous User",
            "https://twitter.com/example/status/123",
            SourceType.SOCIAL_MEDIA,
            "Twitter"
        );
        ContentAnalysis analysis1 = createAnalysis(
            item1,
            true,
            ViolationCategory.STEREOTYPING,
            0.95,
            "This statement promotes harmful generalization and false association between Islam and terrorism.",
            "The Quran explicitly condemns aggression: 'Whoever kills a soul unless for a soul or for corruption [done] in the land - it is as if he had slain mankind entirely.' (Quran 5:32). The Prophet Muhammad ﷺ taught mercy and compassion for all."
        );

        // Sample Case 2: Religious Misrepresentation
        ContentItem item2 = createContentItem(
            "Islam forces women to wear burqas and treats them as second-class citizens.",
            "Facebook Post",
            "John Doe",
            "https://facebook.com/posts/456",
            SourceType.SOCIAL_MEDIA,
            "Facebook"
        );
        ContentAnalysis analysis2 = createAnalysis(
            item2,
            true,
            ViolationCategory.MISREPRESENTATION,
            0.88,
            "This misrepresents Islamic teachings on women's rights and clothing choices.",
            "The Quran states: 'There is no compulsion in religion.' (2:256). Hijab is a personal choice and spiritual practice. Islam granted women rights to education, property ownership, and divorce 1400 years ago. The Prophet Muhammad ﷺ said: 'The best of you are those who are best to their women.'"
        );

        // Sample Case 3: Dehumanization
        ContentItem item3 = createContentItem(
            "Muslims are invading our country and replacing our culture. They're like animals.",
            "YouTube Comment",
            "User12345",
            "https://youtube.com/watch?v=abc#comment789",
            SourceType.SOCIAL_MEDIA,
            "YouTube"
        );
        ContentAnalysis analysis3 = createAnalysis(
            item3,
            true,
            ViolationCategory.DEHUMANIZATION,
            0.92,
            "Uses dehumanizing language ('animals') and promotes replacement theory conspiracy.",
            "The Quran teaches: 'O mankind, indeed We have created you from male and female and made you peoples and tribes that you may know one another.' (49:13). The Prophet Muhammad ﷺ said in his Farewell Sermon: 'All mankind is from Adam and Eve. An Arab has no superiority over a non-Arab, nor does a non-Arab have any superiority over an Arab... except by piety and good action.'"
        );

        // Sample Case 4: Political Conspiracy
        ContentItem item4 = createContentItem(
            "Muslims are secretly implementing Sharia law to take over Western governments.",
            "Blog Article",
            "Conspiracy Watch",
            "https://example-blog.com/sharia-conspiracy",
            SourceType.NEWS_ARTICLE,
            "Independent Blog"
        );
        ContentAnalysis analysis4 = createAnalysis(
            item4,
            true,
            ViolationCategory.CONSPIRACY_THEORY,
            0.87,
            "Promotes unfounded conspiracy theory about Sharia law implementation.",
            "Sharia refers to Islamic moral and ethical guidelines for personal worship and conduct. In Western democracies, Muslims follow civil laws while practicing their faith personally. The claim of 'secret takeover' is a baseless conspiracy theory with no evidence. Muslim communities actively participate in democratic processes and civic life."
        );

        // Sample Case 5: Historical Distortion
        ContentItem item5 = createContentItem(
            "Islamic civilization contributed nothing to science. All their achievements were stolen from Greeks.",
            "Forum Post",
            "HistoryBuff2024",
            "https://historyforum.net/thread/islamic-science",
            SourceType.FORUM,
            "History Forum"
        );
        ContentAnalysis analysis5 = createAnalysis(
            item5,
            true,
            ViolationCategory.HISTORICAL_REVISIONISM,
            0.90,
            "Erases significant contributions of Islamic Golden Age to science and civilization.",
            "Islamic scholars preserved AND advanced Greek knowledge while making original contributions: Algebra (Al-Khwarizmi), optics (Ibn al-Haytham), medicine (Ibn Sina/Avicenna), astronomy, chemistry, and philosophy. These works were translated to Latin and fueled the European Renaissance. The House of Wisdom in Baghdad was a beacon of learning when much of Europe was in the Dark Ages."
        );

        // Save all analyses (which also saves associated content items)
        analysisRepository.saveAll(List.of(analysis1, analysis2, analysis3, analysis4, analysis5));
    }

    private ContentItem createContentItem(String content, String source, String author, 
                                         String url, SourceType sourceType, String platform) {
        ContentItem item = new ContentItem();
        item.setContent(content);
        item.setTitle(source);
        item.setSource(url);
        item.setAuthor(author);
        item.setSourceType(sourceType);
        item.setSourcePlatform(platform);
        item.setDetectedAt(java.time.Instant.now());
        return item;
    }

    private ContentAnalysis createAnalysis(ContentItem item, boolean isViolation,
                                          ViolationCategory category, double confidence,
                                          String explanation, String scholarlyRefutation) {
        ContentAnalysis analysis = new ContentAnalysis();
        analysis.setContentItem(item);
        analysis.setConfirmedViolation(isViolation);
        analysis.setCategory(category);
        analysis.setViolationConfidence(java.math.BigDecimal.valueOf(confidence));
        analysis.setViolationExplanation(explanation);
        analysis.setCounterArgument(scholarlyRefutation);
        analysis.setAnalyzedAt(java.time.Instant.now());
        return analysis;
    }
}
