# Developer Runbook

## 1. Fast onboarding

### Check your machine
```bash
./scripts/onboard.sh doctor
```

### Generate `.env`
```bash
./scripts/onboard.sh init-env
```

### Start the Docker stack
```bash
./scripts/onboard.sh up
```

### Or run locally
```bash
./scripts/onboard.sh run-local
```

### Print service URLs
```bash
./scripts/onboard.sh services
```

## 2. Recommended learning modes

### Mode A: safest onboarding
Use mock AI first.

`.env`
```dotenv
LLM_PROVIDER=none
APP_SAMPLE_DATA_ENABLED=true
APP_CONTENT_FETCH_ENABLED=false
APP_CONTENT_BACKFILL_ENABLED=false
```

Why:
- no API keys needed
- no Ollama install required
- easiest way to understand the UI and persistence flow

### Mode B: local Ollama
Use when you want to learn the AI path without cloud cost.

Local run defaults:
```dotenv
LLM_PROVIDER=ollama
OLLAMA_HOST=http://localhost:11434
OLLAMA_MODEL=llama3.1
```

Important:
```bash
ollama pull llama3.1
```

### Mode C: Docker Compose + Ollama container
Use when you want everything isolated.

Compose-oriented defaults:
```dotenv
LLM_PROVIDER=ollama
OLLAMA_HOST=http://ollama:11434
OLLAMA_MODEL=llama3.1
DATABASE_URL=jdbc:postgresql://postgres:5432/islamophobia_detector
```

## 3. Service map

| Service | URL / Port | Notes |
|---|---|---|
| App UI/API | `http://localhost:8080` | Main UI and REST API |
| Health | `http://localhost:8080/actuator/health` | Quick readiness check |
| Prometheus | `http://localhost:9090` | Metrics scraping |
| Grafana | `http://localhost:3000` | Dashboards |
| Jaeger | `http://localhost:16686` | Trace UI |
| PostgreSQL | `localhost:5432` | Default user/pass `postgres/postgres` |
| Ollama | `http://localhost:11434` | Only when using Ollama |

Grafana note: the compose file does not currently override admin credentials. With the stock image, first login is commonly `admin/admin`, followed by a password reset prompt.

## 4. Day-1 troubleshooting

### Problem: `model '...' not found`
Cause:
- Ollama is reachable, but the model tag configured in `OLLAMA_MODEL` does not exist in that Ollama instance.

Fix:
```bash
ollama list
ollama pull llama3.1
```

If using Dockerized Ollama:
```bash
docker exec -it islamophobia-ollama ollama list
docker exec -it islamophobia-ollama ollama pull llama3.1
```

### Problem: repeated scheduler errors every minute
Cause:
- pending rows are being retried by `PendingContentAnalysisScheduler`
- AI provider is down or misconfigured

Quick containment:
```dotenv
APP_CONTENT_BACKFILL_ENABLED=false
```

### Problem: list shows only seeded demo content
Cause:
- sample data is enabled
- or feed fetching is disabled
- or the app has not yet processed live feed items

Options:
```dotenv
APP_SAMPLE_DATA_ENABLED=false
APP_CONTENT_FETCH_ENABLED=true
APP_CONTENT_BACKFILL_ENABLED=true
```

### Problem: live feed items do not appear
Check:
- the RSS URL is real XML, not HTML
- the content matches monitored keywords
- the item is not already persisted by `sourceUrl`

## 5. Common developer commands

### Run tests
```bash
mvn test
```

### Package the app
```bash
mvn clean package
```

### Start only infra with Docker
```bash
docker compose -f docker/docker-compose.yml up -d postgres prometheus grafana jaeger
```

### Tail app logs in Docker
```bash
docker compose -f docker/docker-compose.yml logs -f app
```

### Check the database quickly
```bash
docker exec -it islamophobia-db psql -U postgres -d islamophobia_detector
```

Useful SQL:
```sql
select count(*) from content_item;
select count(*) from content_analysis;
select id, title, source_url, detected_at from content_item order by detected_at desc limit 20;
select id, model_used, is_confirmed_violation, analyzed_at from content_analysis order by analyzed_at desc limit 20;
```

## 6. How to reason about the pipeline

If a row exists in `content_item` but not in `content_analysis`, the break is between:
- `ContentFetcherService` / controller input
- and `IslamicContentAnalyzer`

If a row exists in `content_analysis` but does not appear in the main grid, check:
- whether it is a confirmed violation
- whether the UI is falling back to recent analyses
- whether category filtering is too restrictive

## 7. Recommended next engineering tasks

1. add an admin endpoint to trigger feed fetch manually
2. move RSS sources into config rather than hard-coded Java
3. add integration tests around fallback analysis and scheduler behavior
4. expose feed ingestion stats in the UI
5. add a dedicated “raw content explorer” page separate from the confirmed-violations dashboard

