# Setup Guide

Use this file for the shortest path to a working local environment.

## Checklist
- [ ] Check dependencies
- [ ] Generate `.env`
- [ ] Choose mock, Ollama, or OpenAI mode
- [ ] Start services
- [ ] Verify app health

## Option A: easiest onboarding

Run without a real AI provider first.

```bash
./scripts/onboard.sh doctor
./scripts/onboard.sh init-env
./scripts/onboard.sh run-local
```

Recommended `.env` values:
```dotenv
LLM_PROVIDER=none
APP_CONTENT_FETCH_ENABLED=false
APP_CONTENT_BACKFILL_ENABLED=false
APP_SAMPLE_DATA_ENABLED=true
```

## Option B: local Ollama

1. Install Ollama
2. Pull a model
3. Run the app

```bash
ollama pull llama3.1
./scripts/onboard.sh init-env
./scripts/onboard.sh run-local
```

Recommended `.env` values:
```dotenv
LLM_PROVIDER=ollama
OLLAMA_HOST=http://localhost:11434
OLLAMA_MODEL=llama3.1
DATABASE_URL=jdbc:postgresql://localhost:5432/islamophobia_detector
```

## Option C: full Docker stack

```bash
./scripts/onboard.sh init-env
./scripts/onboard.sh up
```

Recommended `.env` values for compose:
```dotenv
LLM_PROVIDER=ollama
OLLAMA_HOST=http://ollama:11434
OLLAMA_MODEL=llama3.1
DATABASE_URL=jdbc:postgresql://postgres:5432/islamophobia_detector
```

## Verify the app

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/violations?page=0&size=5
```

## Fix the exact Ollama error from your logs

If you see:
- `model '...' not found`

Run:
```bash
ollama list
ollama pull llama3.1
```

Or with Dockerized Ollama:
```bash
docker exec -it islamophobia-ollama ollama list
docker exec -it islamophobia-ollama ollama pull llama3.1
```

## Useful links

- Main overview: `README.md`
- Architecture: `docs/ARCHITECTURE.md`
- Developer runbook: `docs/DEVELOPER_RUNBOOK.md`
- AI/ML exercises: `docs/AI_ML_FOUNDATIONS.md`
- UI guide: `docs/UI_EXTENSION_GUIDE.md`
