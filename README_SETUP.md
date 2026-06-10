# Islamophobia Detector - Setup & Running Guide

## 🚀 Quick Start

### Option 1: Using OpenAI (Recommended for Production)

1. **Set up your environment:**
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` file:**
   ```env
   LLM_PROVIDER=openai
   OPENAI_API_KEY=sk-proj-your-full-api-key-here
   OPENAI_MODEL=gpt-4o
   DATABASE_URL=jdbc:postgresql://localhost:5432/islamophobia_detector
   DATABASE_USERNAME=postgres
   DATABASE_PASSWORD=postgres
   ```

3. **Start PostgreSQL (using Docker):**
   ```bash
   docker run -d --name postgres-islamophobia \
     -e POSTGRES_DB=islamophobia_detector \
     -e POSTGRES_USER=postgres \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 \
     postgres:15-alpine
   ```

4. **Run the application:**
   ```bash
   # Load environment variables
   export $(cat .env | xargs)
   
   # Build and run with Maven
   ./mvnw clean package -DskipTests
   java -jar target/islamophobia-detector-1.0.0-SNAPSHOT.jar
   
   # OR using Docker Compose (recommended)
   docker-compose up -d app
   ```

5. **Access the UI:** http://localhost:8080

---

### Option 2: Using Ollama with Llama (Free & Local)

1. **Install Ollama:**
   - macOS/Linux: `curl -fsSL https://ollama.com/install.sh | sh`
   - Windows: Download from https://ollama.com

2. **Pull Llama model:**
   ```bash
   ollama pull llama3.1:8b-instruct-q4_0
   ```

3. **Verify Ollama is running:**
   ```bash
   ollama list
   # Should show llama3.1:8b-instruct-q4_0
   ```

4. **Set up environment:**
   ```bash
   cp .env.example .env
   # Edit .env:
   LLM_PROVIDER=ollama
   OLLAMA_HOST=http://localhost:11434
   OLLAMA_MODEL=llama3.1:8b-instruct-q4_0
   ```

5. **Start all services with Docker Compose:**
   ```bash
   docker-compose up -d
   ```

6. **Access the UI:** http://localhost:8080

---

## 📋 Environment Variables Reference

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `LLM_PROVIDER` | Choose `openai` or `ollama` | `openai` | No |
| `OPENAI_API_KEY` | Your OpenAI API key | - | Yes (if using OpenAI) |
| `OPENAI_MODEL` | OpenAI model name | `gpt-4o` | No |
| `OLLAMA_HOST` | Ollama server URL | `http://localhost:11434` | No |
| `OLLAMA_MODEL` | Ollama model name | `llama3.1:8b-instruct-q4_0` | No |
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/islamophobia_detector` | No |
| `DATABASE_USERNAME` | Database username | `postgres` | No |
| `DATABASE_PASSWORD` | Database password | `postgres` | No |
| `SERVER_PORT` | Application port | `8080` | No |

---

## 🐳 Docker Compose Services

The `docker-compose.yml` includes:

- **app**: Spring Boot application
- **postgres**: PostgreSQL database
- **prometheus**: Metrics collection
- **grafana**: Visualization dashboards
- **jaeger**: Distributed tracing
- **ollama**: (Optional) Local LLM server

### Start all services:
```bash
docker-compose up -d
```

### View logs:
```bash
docker-compose logs -f app
```

### Stop all services:
```bash
docker-compose down
```

---

## 🔍 Testing the Application

### 1. Test Content Analysis API:
```bash
curl -X POST http://localhost:8080/api/v1/content/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Sample content to analyze",
    "sourceType": "SOCIAL_MEDIA",
    "platform": "Twitter",
    "url": "https://twitter.com/example/status/123"
  }'
```

### 2. Get Violations Feed:
```bash
curl http://localhost:8080/api/v1/violations?page=0&size=10
```

### 3. Submit Feedback:
```bash
curl -X POST http://localhost:8080/api/v1/analysis/{analysisId}/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "feedbackType": "LIKE",
    "comment": "Great analysis!"
  }'
```

### 4. Access Vaadin UI:
Open http://localhost:8080 in your browser

### 5. Monitoring Dashboards:
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Jaeger**: http://localhost:16686

---

## 🛠️ Troubleshooting

### Issue: "Connection refused" to Ollama
**Solution:** Ensure Ollama is running:
```bash
ollama serve
# Or restart Ollama service
```

### Issue: OpenAI API errors
**Solution:** 
- Verify your API key is correct and has credits
- Check network connectivity
- Try a different model: `OPENAI_MODEL=gpt-3.5-turbo`

### Issue: Database connection failed
**Solution:**
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Restart database
docker-compose restart postgres
```

### Issue: High memory usage with Ollama
**Solution:** Use a smaller model:
```bash
ollama pull llama3.1:8b-instruct-q4_0
# Update .env: OLLAMA_MODEL=llama3.1:8b-instruct-q4_0
```

---

## 📊 Comparing LLM Providers

| Feature | OpenAI (GPT-4) | Ollama (Llama 3.1) |
|---------|----------------|-------------------|
| **Cost** | Paid (~$0.03/1K tokens) | Free |
| **Speed** | Fast (API) | Depends on hardware |
| **Accuracy** | Very High | Good |
| **Privacy** | Data sent to OpenAI | Fully local |
| **Setup** | Easy (API key) | Requires Ollama install |
| **Best For** | Production, high accuracy | Development, privacy, cost-saving |

---

## 🎯 Next Steps

1. **Configure your preferred LLM** (OpenAI or Ollama)
2. **Start the services** using Docker Compose
3. **Access the UI** at http://localhost:8080
4. **Test content analysis** via API or UI
5. **Monitor performance** via Grafana dashboards

For more details, see `PULL_REQUEST.md` and the API documentation.
