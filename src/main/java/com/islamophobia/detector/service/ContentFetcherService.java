package com.islamophobia.detector.service;

import com.islamophobia.detector.model.entity.ContentItem;
import com.islamophobia.detector.model.enums.SourceType;
import com.islamophobia.detector.repository.ContentItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service to fetch content from RSS feeds, social media, and news sources
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentFetcherService {

    private final ContentItemRepository contentItemRepository;
    private final ContentAnalysisService contentAnalysisService;
    private final RestTemplate restTemplate;

    @Value("${app.content.fetch-timeout-seconds:30}")
    private int fetchTimeoutSeconds;

    @Value("${app.content.max-content-length:50000}")
    private int maxContentLength;

    // Track processed URLs to avoid duplicates
    private final Set<String> processedUrls = ConcurrentHashMap.newKeySet();

    // Default RSS feeds for Islam-related news and discussions
    // Removed Twitter blog feed as it's protected by Cloudflare and not relevant for hate speech detection
    private static final List<String> DEFAULT_RSS_FEEDS = List.of(
        // Major news outlets with comprehensive world coverage
        "https://www.aljazeera.com/xml/rss/all.xml",
        "https://feeds.reuters.com/reuters/worldNews",
        "https://rss.nytimes.com/services/xml/rss/nyt/World.xml",
        "https://feeds.bbci.co.uk/news/world/rss.xml",
        "https://www.theguardian.com/world/rss",
        "https://www.washingtonpost.com/rss/world",
        "https://www.cnn.com/services/rss/",
        // Islamic news and community sources
        "https://www.islamicity.org/feed/",
        "https://muslimmatters.org/feed/",
        "https://www.islamweb.net/en/rss/index.xml",
        "https://www.yaqeeninstitute.org/feed/",
        // Human rights and religious freedom monitoring
        "https://www.hrw.org/rss/releases/religious-freedom",
        "https://www.state.gov/feed/"
    );

    // Keywords to identify potentially relevant content
    private static final List<String> MONITORING_KEYWORDS = List.of(
        "Muslim", "Islam", "Islamic", "Mosque", "Quran", "Hadith",
        "Hijab", "Burqa", "Sharia", "Jihad", "Allah", "Prophet Muhammad",
        "Ramadan", "Eid", "Halal", "Ummah", "Caliphate"
    );

    /**
     * Scheduled task to fetch content from RSS feeds every 30 minutes
     */
    @Scheduled(fixedRate = 1800000) // 30 minutes
    public void fetchFromRssFeeds() {
        log.info("Starting RSS feed content fetching...");
        
        List<String> feedsToProcess = getActiveRssFeeds();
        
        for (String feedUrl : feedsToProcess) {
            try {
                fetchAndProcessRssFeed(feedUrl);
            } catch (Exception e) {
                log.error("Error processing RSS feed {}: {}", feedUrl, e.getMessage());
            }
        }
        
        log.info("RSS feed fetching completed. Processed {} feeds.", feedsToProcess.size());
    }

    /**
     * Fetch and process a single RSS feed
     */
    private void fetchAndProcessRssFeed(String feedUrl) {
        try {
            log.info("Fetching RSS feed: {}", feedUrl);
            
            // Set up headers to mimic a browser request
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            headers.set("Accept", "application/rss+xml, application/xml, text/xml, */*");
            headers.set("Accept-Language", "en-US,en;q=0.9");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String feedContent = restTemplate.exchange(feedUrl, org.springframework.http.HttpMethod.GET, entity, String.class).getBody();
            
            if (feedContent == null || feedContent.isEmpty()) {
                log.warn("Empty response from RSS feed: {}", feedUrl);
                return;
            }

            // Check if response is HTML (Cloudflare protection or error page)
            if (feedContent.trim().startsWith("<!DOCTYPE html>") || feedContent.trim().startsWith("<html")) {
                log.warn("Received HTML instead of XML from {}. Possible Cloudflare protection or error page. Skipping.", feedUrl);
                return;
            }

            Document doc = Jsoup.parse(feedContent, "", org.jsoup.parser.Parser.xmlParser());
            List<Element> items = doc.select("item");

            log.info("Found {} items in RSS feed: {}", items.size(), feedUrl);

            for (Element item : items) {
                processRssItem(item, feedUrl);
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP client error fetching RSS feed {}: {} - Skipping this feed", feedUrl, e.getStatusCode());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("HTTP server error fetching RSS feed {}: {} - Skipping this feed", feedUrl, e.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to parse RSS feed {}: {} - Skipping", feedUrl, e.getMessage());
        }
    }

    /**
     * Process a single RSS item
     */
    private void processRssItem(Element item, String feedUrl) {
        String title = item.selectFirst("title") != null ? 
            item.selectFirst("title").text() : "";
        String description = item.selectFirst("description") != null ? 
            item.selectFirst("description").text() : "";
        String link = item.selectFirst("link") != null ? 
            item.selectFirst("link").text() : "";
        String pubDate = item.selectFirst("pubDate") != null ? 
            item.selectFirst("pubDate").text() : LocalDateTime.now().toString();
        String author = item.selectFirst("author") != null ? 
            item.selectFirst("author").text() : "RSS Feed";

        // Skip if already processed
        if (processedUrls.contains(link) || !isRelevantContent(title, description)) {
            return;
        }

        try {
            // Combine title and description for analysis
            String content = title + "\n\n" + truncateContent(description, maxContentLength);

            // Create content item
            ContentItem contentItem = new ContentItem();
            contentItem.setContent(content);
            contentItem.setTitle(title);
            contentItem.setSource(feedUrl);
            contentItem.setAuthor(author);
            contentItem.setSourceType(SourceType.NEWS_ARTICLE);
            contentItem.setSourcePlatform(extractPlatformFromUrl(feedUrl));
            contentItem.setDetectedAt(java.time.Instant.now());
            contentItem.setMetadata(Map.of(
                "publishedDate", pubDate,
                "feedUrl", feedUrl
            ));

            // Save and analyze
            ContentItem savedItem = contentItemRepository.save(contentItem);
            processedUrls.add(link);
            
            log.info("Saved new content item: {} - {}", savedItem.getId(), title);
            
            // Trigger AI analysis asynchronously
            try {
                contentAnalysisService.processContent(savedItem);
                log.info("Analysis triggered for content: {}", savedItem.getId());
            } catch (Exception e) {
                log.error("Failed to analyze content {}: {}", savedItem.getId(), e.getMessage());
            }

        } catch (Exception e) {
            log.error("Error processing RSS item: {}", e.getMessage(), e);
        }
    }

    /**
     * Fetch content from web pages (for social media and other sources)
     */
    public ContentItem fetchFromUrl(String url, SourceType sourceType, String platform) {
        if (processedUrls.contains(url)) {
            log.info("URL already processed: {}", url);
            return null;
        }

        try {
            String htmlContent = restTemplate.getForObject(url, String.class);
            if (htmlContent == null) {
                return null;
            }

            Document doc = Jsoup.parse(htmlContent);
            
            // Extract main content (simplified extraction)
            String title = doc.title();
            String content = extractMainContent(doc);
            String author = extractAuthor(doc);

            if (content == null || content.trim().isEmpty()) {
                content = doc.body().text();
            }

            ContentItem contentItem = new ContentItem();
            contentItem.setContent(truncateContent(content, maxContentLength));
            contentItem.setTitle(title);
            contentItem.setSource(url);
            contentItem.setAuthor(author != null ? author : "Web Scraping");
            contentItem.setSourceType(sourceType);
            contentItem.setSourcePlatform(platform);
            contentItem.setDetectedAt(java.time.Instant.now());

            ContentItem savedItem = contentItemRepository.save(contentItem);
            processedUrls.add(url);
            
            log.info("Fetched content from URL: {} - {}", url, title);
            
            return savedItem;

        } catch (Exception e) {
            log.error("Failed to fetch content from URL {}: {}", url, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Manually trigger content fetch from all sources
     */
    public void triggerManualFetch() {
        log.info("Manual content fetch triggered");
        fetchFromRssFeeds();
    }

    /**
     * Check if content contains relevant keywords
     */
    private boolean isRelevantContent(String title, String description) {
        String combinedText = (title + " " + description).toLowerCase();
        
        return MONITORING_KEYWORDS.stream()
            .anyMatch(keyword -> combinedText.contains(keyword.toLowerCase()));
    }

    /**
     * Extract main content from HTML document
     */
    private String extractMainContent(Document doc) {
        // Try common content containers
        String[] contentSelectors = {
            "article", ".post-content", ".entry-content", ".article-body",
            ".content", "#content", "main", ".story-content"
        };

        for (String selector : contentSelectors) {
            Element contentElement = doc.selectFirst(selector);
            if (contentElement != null) {
                String text = contentElement.text();
                if (text.length() > 50) {
                    return text;
                }
            }
        }

        // Fallback to body text
        return doc.body() != null ? doc.body().text() : "";
    }

    /**
     * Extract author from HTML document
     */
    private String extractAuthor(Document doc) {
        String[] authorSelectors = {
            ".author", ".byline", "[rel='author']", "meta[name='author']",
            ".article-author", ".post-author"
        };

        for (String selector : authorSelectors) {
            Element authorElement = doc.selectFirst(selector);
            if (authorElement != null) {
                String author = authorElement.attr("content");
                if (author == null || author.isEmpty()) {
                    author = authorElement.text();
                }
                if (!author.isEmpty()) {
                    return author;
                }
            }
        }

        return null;
    }

    /**
     * Get active RSS feeds from configuration or defaults
     */
    private List<String> getActiveRssFeeds() {
        // In production, this could be loaded from database or configuration
        return DEFAULT_RSS_FEEDS;
    }

    /**
     * Extract platform name from URL
     */
    private String extractPlatformFromUrl(String url) {
        try {
            String host = new URL(url).getHost();
            if (host.contains("twitter") || host.contains("x.com")) {
                return "Twitter/X";
            } else if (host.contains("facebook")) {
                return "Facebook";
            } else if (host.contains("youtube")) {
                return "YouTube";
            } else if (host.contains("bbc")) {
                return "BBC News";
            } else if (host.contains("reuters")) {
                return "Reuters";
            } else if (host.contains("aljazeera")) {
                return "Al Jazeera";
            } else if (host.contains("nytimes")) {
                return "New York Times";
            } else {
                return host;
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Truncate content to maximum length
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * Add custom RSS feed to monitor
     */
    public void addRssFeed(String feedUrl) {
        if (!DEFAULT_RSS_FEEDS.contains(feedUrl)) {
            log.info("Adding new RSS feed to monitor: {}", feedUrl);
            // In production, save to database
        }
    }

    /**
     * Remove RSS feed from monitoring
     */
    public void removeRssFeed(String feedUrl) {
        log.info("Removing RSS feed from monitoring: {}", feedUrl);
        // In production, remove from database
    }

    /**
     * Get count of processed URLs
     */
    public int getProcessedUrlCount() {
        return processedUrls.size();
    }

    /**
     * Clear processed URLs cache (useful for testing or re-fetching)
     */
    public void clearProcessedUrls() {
        int cleared = processedUrls.size();
        processedUrls.clear();
        log.info("Cleared {} processed URLs from cache", cleared);
    }
}
