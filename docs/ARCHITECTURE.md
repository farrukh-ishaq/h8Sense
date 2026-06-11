# Architecture Guide

## What this system does

`h8Sense` collects content from configured sources, stores the raw content, runs an LLM-based analysis step, and presents the results in a Vaadin UI and REST API.

## Main runtime components

### 1. Ingestion
- `ContentFetcherService`
- Scheduled RSS polling via `@Scheduled`
- Filters incoming feed items by keyword relevance
- Persists `ContentItem` rows before analysis starts

### 2. Analysis
- `ContentAnalysisService`
- `IslamicContentAnalyzer`
- `LlmConfig`
- `MockChatModel`

The flow is:
1. create or fetch a `ContentItem`
2. build prompt context
3. call the configured `ChatModel`
4. parse structured JSON into `AnalysisResult`
5. store a `ContentAnalysis`

### 3. Backfill / recovery
- `PendingContentAnalysisScheduler`

This scheduler keeps retrying content items that have no `ContentAnalysis` yet. This is why a broken AI provider can flood logs repeatedly.

### 4. UI
- `MainView`
- Vaadin server-side components rendered by Spring Boot

The UI shows:
- summary cards
- a grid of analyses
- a detail panel
- feedback buttons

### 5. Storage
- PostgreSQL in normal mode
- H2 for `dev` profile
- JPA entities: `ContentItem`, `ContentAnalysis`, `UserFeedback`, `FeedbackTracking`

### 6. Observability
- Spring Boot Actuator
- Prometheus
- Grafana
- Jaeger container present in compose

## Key code paths

### Manual API analysis
`POST /api/v1/content/analyze`

`ContentAnalysisController` -> `ContentAnalysisService.processContent()` -> `IslamicContentAnalyzer.analyzeContent()`

### Background feed flow
`ContentFetcherService.fetchFromRssFeeds()` -> save `ContentItem` -> `ContentAnalysisService.processContent()`

### Recovery flow
`PendingContentAnalysisScheduler.backfillPendingContent()` -> `ContentAnalysisService.processContent()`

## Why the Ollama error was happening

The app was configured with an Ollama model tag that was not present in the target Ollama instance:

- old default: `llama3.1:8b-instruct-q4_0`
- safer default now: `llama3.1`

A missing Ollama model returns HTTP 404 from the Ollama API. Before this fix, that exception bubbled up and the scheduler kept retrying the same rows. After the fix:
- the content is still persisted
- the analyzer stores a safe fallback analysis instead of crashing
- logs still tell the operator what to fix

## Why “all sources were 404”

There were two overlapping causes:

1. sample data used synthetic placeholder links, which looked like broken real sources
2. several configured RSS endpoints now return HTML, timeout pages, or stale content rather than valid RSS XML

This repo now:
- removes dead placeholder source links from seeded sample data
- trims the default RSS list to verified XML feeds

## Default verified RSS feeds

As of `2026-06-11`, these defaults were verified to return XML/RSS:
- `https://www.aljazeera.com/xml/rss/all.xml`
- `https://rss.nytimes.com/services/xml/rss/nyt/World.xml`
- `https://feeds.bbci.co.uk/news/world/rss.xml`
- `https://www.theguardian.com/world/rss`
- `https://muslimmatters.org/feed/`

## Important configuration switches

### LLM
- `LLM_PROVIDER=none|openai|ollama`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `OLLAMA_HOST`
- `OLLAMA_MODEL`

### Content pipeline
- `APP_CONTENT_FETCH_ENABLED=true|false`
- `APP_CONTENT_BACKFILL_ENABLED=true|false`
- `APP_SAMPLE_DATA_ENABLED=true|false`

## Good first extension points

### Add a new feed source
Edit `ContentFetcherService.DEFAULT_RSS_FEEDS`

### Change the prompt or output format
Edit `IslamicContentAnalyzer`

### Improve category mapping
Edit `ContentAnalysisService.resolveCategory()` and `ViolationCategory`

### Extend the dashboard
Edit `MainView`

### Add REST endpoints
Edit `ContentAnalysisController`

