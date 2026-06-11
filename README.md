# h8Sense

AI-assisted content ingestion and analysis for detecting Islamophobic content, storing the results, and exploring them through a Vaadin UI and REST API.

## Quick status

This repo now includes fixes for two onboarding blockers:
- safer Ollama defaults and graceful fallback when a configured model is missing
- a cleaned default RSS list plus seeded sample data without dead placeholder source links

## Checklist
- [x] Run the app in mock, Ollama, or OpenAI mode
- [x] Inspect the architecture and main code paths
- [x] Use an onboarding script to generate `.env`, build, and run
- [x] Follow guided AI/ML and UI extension exercises

## Quick start

### 1. Check your machine
```bash
./scripts/onboard.sh doctor
```

### 2. Create or update `.env`
```bash
./scripts/onboard.sh init-env
```

### 3A. Start the full Docker stack
```bash
./scripts/onboard.sh up
```

### 3B. Or run locally
```bash
./scripts/onboard.sh run-local
```

### 4. Print service URLs
```bash
./scripts/onboard.sh services
```

## URLs

| Service | URL |
|---|---|
| App UI/API | `http://localhost:8080` |
| Health | `http://localhost:8080/actuator/health` |
| Prometheus | `http://localhost:9090` |
| Grafana | `http://localhost:3000` |
| Jaeger | `http://localhost:16686` |
| Ollama | `http://localhost:11434` |

## Most important environment variables

| Variable | Meaning | Common values |
|---|---|---|
| `LLM_PROVIDER` | AI provider mode | `none`, `ollama`, `openai` |
| `OPENAI_API_KEY` | OpenAI key | secret |
| `OPENAI_MODEL` | OpenAI model | `gpt-4o` |
| `OLLAMA_HOST` | Ollama base URL | `http://localhost:11434`, `http://ollama:11434` |
| `OLLAMA_MODEL` | Ollama model tag | `llama3.1` |
| `DATABASE_URL` | JDBC connection string | local or compose Postgres |
| `APP_CONTENT_FETCH_ENABLED` | background feed fetcher | `true` / `false` |
| `APP_CONTENT_BACKFILL_ENABLED` | pending analysis scheduler | `true` / `false` |
| `APP_SAMPLE_DATA_ENABLED` | seed demo data on empty DB | `true` / `false` |

## Recommended onboarding modes

### Mode 1: Learn the system without AI dependencies
```dotenv
LLM_PROVIDER=none
APP_CONTENT_FETCH_ENABLED=false
APP_CONTENT_BACKFILL_ENABLED=false
APP_SAMPLE_DATA_ENABLED=true
```

### Mode 2: Learn the AI path with local Ollama
```dotenv
LLM_PROVIDER=ollama
OLLAMA_HOST=http://localhost:11434
OLLAMA_MODEL=llama3.1
```

Before running with Ollama:
```bash
ollama pull llama3.1
```

### Mode 3: Cloud-backed analysis
```dotenv
LLM_PROVIDER=openai
OPENAI_API_KEY=...
OPENAI_MODEL=gpt-4o
```

## Why the model was not loading

Your log shows this exact failure mode:
- the app can reach Ollama
- the configured model does not exist there
- Ollama returns HTTP `404 model not found`

Typical fixes:
```bash
ollama list
ollama pull llama3.1
```

If using Dockerized Ollama:
```bash
docker exec -it islamophobia-ollama ollama list
docker exec -it islamophobia-ollama ollama pull llama3.1
```

The code now falls back to a safe stored analysis instead of repeatedly crashing the backfill scheduler.

## Why source links were misleading

There were two issues:
1. seeded sample records used synthetic placeholder links
2. some default RSS entries were stale and no longer returned RSS XML

What changed:
- seeded sample data no longer renders dead placeholder source links
- default feed list was reduced to verified RSS/XML endpoints

## Verified default RSS feeds

- `https://www.aljazeera.com/xml/rss/all.xml`
- `https://rss.nytimes.com/services/xml/rss/nyt/World.xml`
- `https://feeds.bbci.co.uk/news/world/rss.xml`
- `https://www.theguardian.com/world/rss`
- `https://muslimmatters.org/feed/`

## Core components

| Area | Main files |
|---|---|
| App bootstrap | `src/main/java/com/islamophobia/detector/IslamophobiaDetectorApplication.java` |
| LLM config | `src/main/java/com/islamophobia/detector/config/LlmConfig.java` |
| Analyzer | `src/main/java/com/islamophobia/detector/ai/IslamicContentAnalyzer.java` |
| Content pipeline | `src/main/java/com/islamophobia/detector/service/ContentAnalysisService.java` |
| Feed ingestion | `src/main/java/com/islamophobia/detector/service/ContentFetcherService.java` |
| Backfill scheduler | `src/main/java/com/islamophobia/detector/service/PendingContentAnalysisScheduler.java` |
| UI | `src/main/java/com/islamophobia/detector/ui/MainView.java` |
| API | `src/main/java/com/islamophobia/detector/controller/ContentAnalysisController.java` |
| Config | `src/main/resources/application.yml` |

## Common commands

### Run tests
```bash
mvn test
```

### Build
```bash
mvn clean package
```

### Analyze one payload
```bash
curl -X POST http://localhost:8080/api/v1/content/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "content": "All Muslims support terrorism and want to destroy our way of life.",
    "sourceType": "SOCIAL_MEDIA",
    "platform": "CLI",
    "author": "demo"
  }'
```

### Read the violations feed
```bash
curl http://localhost:8080/api/v1/violations?page=0&size=10
```

## Extend the project

### Learn the architecture
- `docs/ARCHITECTURE.md`
- `docs/DEVELOPER_RUNBOOK.md`

### Learn AI / ML foundations using this codebase
- `docs/AI_ML_FOUNDATIONS.md`

### Extend the Vaadin UI
- `docs/UI_EXTENSION_GUIDE.md`

## Troubleshooting

### Repeated logs about pending backfill
Disable temporarily:
```dotenv
APP_CONTENT_BACKFILL_ENABLED=false
```

### Only demo rows appear
Likely causes:
- sample data is enabled
- live fetch is disabled
- the fetcher has not yet found relevant items

Use:
```dotenv
APP_SAMPLE_DATA_ENABLED=false
APP_CONTENT_FETCH_ENABLED=true
APP_CONTENT_BACKFILL_ENABLED=true
```

### Local app cannot reach Docker service names
When running the Spring Boot app locally, prefer:
- `DATABASE_URL=jdbc:postgresql://localhost:5432/islamophobia_detector`
- `OLLAMA_HOST=http://localhost:11434`

When running inside Docker Compose, prefer:
- `DATABASE_URL=jdbc:postgresql://postgres:5432/islamophobia_detector`
- `OLLAMA_HOST=http://ollama:11434`

## Project structure

```text
src/main/java/com/islamophobia/detector/
├── ai/
├── config/
├── controller/
├── init/
├── model/
├── repository/
├── service/
└── ui/

docs/
├── AI_ML_FOUNDATIONS.md
├── ARCHITECTURE.md
├── DEVELOPER_RUNBOOK.md
└── UI_EXTENSION_GUIDE.md

scripts/
└── onboard.sh
```

## Next high-value improvements

- move feed URLs to configuration instead of hard-coded Java
- add an operator endpoint for manual feed refresh
- add integration tests for scheduler + fallback behavior
- add a dedicated UI page for raw ingested content
- add evaluation datasets and provider comparison workflows
