# Islamophobia Content Detector

An AI-powered microservice application that automatically detects hate speech and Islamophobic content from online sources (newspapers, social media platforms, etc.) and analyzes it using an LLM trained on Quran and Hadith to provide informed counter-arguments and references.

## 🚀 Quick Start

Get up and running in 5 minutes:

```bash
# 1. Clone and enter directory
git clone https://github.com/farrukh-ishaq/h8Sense.git
cd h8Sense

# 2. Start PostgreSQL with Docker
docker run -d --name postgres-h8sense \
  -e POSTGRES_DB=islamophobia_detector \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine

# 3. Set your OpenAI API key (or use Ollama for local LLM)
export OPENAI_API_KEY=your-openai-api-key-here
# OR for local LLM: export USE_OLLAMA=true

# 4. Build and run
mvn clean package -DskipTests
java -jar target/islamophobia-detector-1.0.0-SNAPSHOT.jar
```

Access the application at **http://localhost:8080**

## 📖 Example Usage

### Testing the AI Analysis API

Once the application is running, you can test content analysis via curl:

```bash
# Analyze a sample hate speech content
curl -X POST http://localhost:8080/api/v1/content/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "content": "All Muslims support terrorism and want to destroy our way of life.",
    "sourceType": "SOCIAL_MEDIA",
    "platform": "Twitter",
    "author": "Anonymous User",
    "url": "https://twitter.com/example/status/123"
  }'
```

**Example Response:**
```json
{
  "id": "uuid-here",
  "contentItem": {
    "id": "uuid-here",
    "content": "All Muslims support terrorism...",
    "sourcePlatform": "Twitter",
    "author": "Anonymous User"
  },
  "isConfirmedViolation": true,
  "category": "TERRORISM_ASSOCIATION",
  "violationConfidence": 0.95,
  "violationExplanation": "This statement makes a sweeping generalization linking all Muslims to terrorism...",
  "counterArgument": "Islam explicitly condemns terrorism. The Quran states: 'Whoever kills a soul unless for a soul or for corruption [done] in the land - it is as if he had slain mankind entirely.' (Quran 5:32)",
  "quranReferences": ["5:32", "2:190"],
  "hadithReferences": ["Sahih Bukhari 9:83:17"],
  "analyzedAt": "2024-06-10T12:00:00Z"
}
```

### Viewing Confirmed Violations

```bash
# Get paginated list of confirmed violations
curl http://localhost:8080/api/v1/violations?page=0&size=10
```

### Submitting Feedback

```bash
# Like an analysis
curl -X POST http://localhost:8080/api/v1/analysis/{analysisId}/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "feedbackType": "LIKE",
    "comment": "Very accurate analysis with proper references"
  }'
```

### Interactive Dashboard

Open **http://localhost:8080** in your browser to access the Vaadin UI dashboard featuring:
- Grid view of all confirmed violations
- Category filters (Theological Misrepresentation, Terrorism Association, etc.)
- Detailed analysis panels with Quran/Hadith references
- Like/Dislike buttons for community feedback
- Real-time feedback statistics

## 🔧 Configuration Options

### Using OpenAI (Cloud)

Create `application.properties` in the root directory:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/islamophobia_detector
spring.datasource.username=postgres
spring.datasource.password=postgres

# OpenAI Configuration
spring.ai.openai.api-key=your-openai-api-key-here
spring.ai.openai.chat.options.model=gpt-4-turbo
```

### Using Ollama (Local LLM)

For local development without API costs:

```bash
# Install Ollama and pull a model
curl -fsSL https://ollama.com/install.sh | sh
ollama pull llama3
```

Then configure:

```properties
# Use Ollama instead of OpenAI
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3
```

### Docker Compose (Full Stack)

For complete setup with monitoring:

```bash
cd docker
docker-compose up -d
```

This starts:
- Application (port 8080)
- PostgreSQL (port 5432)
- Prometheus (port 9090)
- Grafana (port 3000)
- Jaeger (port 16686)

## 🏗️ Architecture Overview

### Content Analysis Flow

1. **Content Collection**: Observers fetch content from social media, news sites, forums
2. **AI Analysis**: Spring AI sends content to LLM with Islamic context prompt
3. **Result Processing**: LLM returns JSON with violation status, category, confidence, and references
4. **Storage**: Results saved to PostgreSQL with full metadata
5. **Display**: Vaadin UI shows confirmed violations in real-time
6. **Feedback**: Users can like/dislike, with anti-spam rate limiting

### Auto-Scanning Setup (Future Enhancement)

To enable automated scanning, implement observers in the `observer` package:

```java
// Example: Twitter observer
@Component
public class TwitterObserver implements ContentObserver {
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void scanTwitter() {
        // Fetch tweets with specific keywords
        // Call /api/v1/content/analyze for each
    }
}
```

## 🐛 Troubleshooting

### Common Issues

**Vaadin UI not loading (403 Forbidden):**
- Clear browser cache
- Check that `/VAADIN/**` is permitted in SecurityConfig
- Rebuild with `mvn clean package -DskipTests`

**AI Analysis failing:**
- Verify OPENAI_API_KEY is set correctly
- Check network connectivity to OpenAI API
- For Ollama, ensure service is running: `ollama list`

**Database connection errors:**
- Ensure PostgreSQL is running: `docker ps | grep postgres`
- Check credentials in application.properties
- Verify database exists: `docker exec -it postgres-h8sense psql -U postgres -l`

**High memory usage:**
- Reduce LLM context size in configuration
- Limit concurrent requests
- Increase Docker container memory limits

## 📊 Monitoring

- **Prometheus**: http://localhost:9090 (metrics)
- **Grafana**: http://localhost:3000 (dashboards, admin/admin)
- **Jaeger**: http://localhost:16686 (distributed tracing)
- **Actuator**: http://localhost:8080/actuator/health

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License.

## Features

### Core Capabilities
- **Automated Content Monitoring**: Fetches content from newspapers, social media, forums, and blogs
- **AI-Powered Analysis**: Uses LLM (GPT-4) with Islamic knowledge base to detect Islamophobia
- **Scholarly Response Generation**: Provides counter-arguments with authentic Quran and Hadith references
- **Violation Metadata Tracking**: Records when, what, by whom violations were detected
- **User Feedback System**: Like/dislike functionality with IP/device fingerprinting to prevent spam
- **Real-time Feed**: Paginated feed of confirmed violations with full analysis
- **Interactive Vaadin UI**: Modern web dashboard for browsing violations and providing feedback

### Technical Architecture
- **Backend**: Java 17 + Spring Boot 3.2
- **UI**: Vaadin 24 (Java-based web UI)
- **Database**: PostgreSQL with JPA/Hibernate
- **AI Integration**: Spring AI with OpenAI
- **Observability**: Micrometer, Prometheus, Grafana, Jaeger
- **Security**: Spring Security with CORS configuration
- **Containerization**: Docker & Docker Compose

## Project Structure

```
islamophobia-detector/
├── src/main/java/com/islamophobia/detector/
│   ├── config/              # Security and app configuration
│   ├── controller/          # REST API endpoints
│   ├── model/
│   │   ├── entity/         # JPA entities (ContentItem, ContentAnalysis, UserFeedback, etc.)
│   │   ├── dto/            # Data transfer objects
│   │   └── enums/          # Enumerations (ViolationCategory, etc.)
│   ├── repository/         # Spring Data repositories
│   ├── service/            # Business logic services
│   ├── ai/                 # AI/LLM integration
│   └── observer/           # Content fetching observers
├── src/main/resources/
│   └── application.yml     # Application configuration
├── docker/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── prometheus.yml
│   └── grafana/
└── pom.xml
```

## Entity Model

### Core Entities

1. **ContentItem**: Represents collected content from various sources
   - Source type (NEWS, SOCIAL_MEDIA, FORUM, etc.)
   - Platform, author, URL
   - Violation flags and confidence scores

2. **ContentAnalysis**: Detailed AI analysis results
   - Violation confirmation status
   - Explanation and counter-arguments
   - Quran and Hadith references
   - LLM metadata

3. **UserFeedback**: User like/dislike feedback
   - IP address tracking
   - Device fingerprinting
   - Comment support

4. **FeedbackTracking**: Anti-spam mechanism
   - Rate limiting per IP/device
   - Automatic blocking for suspicious activity

## API Endpoints

### Content & Analysis
```
GET    /api/v1/violations                    # Get paginated violations feed
GET    /api/v1/content/{id}/analysis         # Get analysis for specific content
```

### Feedback
```
GET    /api/v1/analysis/{id}/feedback        # Get feedback statistics
POST   /api/v1/analysis/{id}/feedback        # Submit like/dislike feedback
```

### Observability
```
GET    /actuator/health                      # Health check
GET    /actuator/prometheus                  # Prometheus metrics
GET    /actuator/info                        # Application info
```

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose (for containerized deployment)
- OpenAI API key

### Option 1: Quick Local Development (Recommended)

1. **Start PostgreSQL Database**
```bash
docker run -d --name postgres-h8sense \
  -e POSTGRES_DB=islamophobia_detector \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15-alpine
```

2. **Set Environment Variables**
```bash
export OPENAI_API_KEY=your-api-key-here
# Optional: Customize database connection
export DATABASE_URL=jdbc:postgresql://localhost:5432/islamophobia_detector
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
```

3. **Build and Run**
```bash
mvn clean package -DskipTests
java -jar target/islamophobia-detector-1.0.0-SNAPSHOT.jar
```

4. **Access the Application**
- **Web UI**: http://localhost:8080
- **REST API**: http://localhost:8080/api/v1/violations
- **Actuator Health**: http://localhost:8080/actuator/health

### Option 2: Full Stack with Docker Compose

1. **Start All Services**
```bash
cd docker
docker-compose up -d
```

This starts:
- PostgreSQL database (port 5432)
- Application (port 8080)
- Prometheus (port 9090)
- Grafana (port 3000)
- Jaeger (port 16686)

2. **Access the Services**
- Application: http://localhost:8080
- Grafana: http://localhost:3000 (admin/admin123)
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686

### Option 3: Development Mode with H2 Database

For quick testing without PostgreSQL:

```bash
# Run with dev profile (uses in-memory H2 database)
java -jar target/islamophobia-detector-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

Access H2 Console at: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:islamophobia_detector`
- Username: `sa`
- Password: (leave empty)

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key for LLM | Required |
| `DATABASE_URL` | PostgreSQL connection URL | jdbc:postgresql://localhost:5432/islamophobia_detector |
| `DATABASE_USERNAME` | Database username | postgres |
| `DATABASE_PASSWORD` | Database password | postgres |
| `SERVER_PORT` | Application port | 8080 |
| `LOG_LEVEL_ROOT` | Root logging level | INFO |
| `LOG_LEVEL_APP` | Application logging level | DEBUG |

### Profiles

- **dev**: H2 in-memory database, debug logging, H2 console enabled
- **prod**: PostgreSQL, optimized logging, validation-only DDL
- **default**: PostgreSQL with update DDL

## Example Usage

### Testing with Sample Data

The application automatically loads 5 sample Islamophobia cases when first started. These include:

1. **Terrorism Association**: False linking of Islam to terrorism
2. **Cultural Superiority**: Claims of Western cultural superiority
3. **Religious Misrepresentation**: Incorrect portrayal of Islamic practices
4. **Immigration Fear-mongering**: Anti-Muslim immigration rhetoric
5. **Sharia Law Misconceptions**: False narratives about Sharia law

### Using the Web UI

1. Navigate to http://localhost:8080
2. Browse confirmed violations in the grid
3. Filter by category (e.g., "TERRORISM_ASSOCIATION")
4. Click on a row to see detailed analysis with Quran/Hadith references
5. Provide feedback using Like/Dislike buttons

### API Examples

**Get all confirmed violations:**
```bash
curl http://localhost:8080/api/v1/violations?page=0&size=10
```

**Get analysis for specific content:**
```bash
curl http://localhost:8080/api/v1/content/1/analysis
```

**Submit feedback:**
```bash
curl -X POST http://localhost:8080/api/v1/analysis/1/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "feedbackType": "LIKE",
    "comment": "Very accurate response with relevant Quran reference!"
  }'
```

**Get feedback statistics:**
```bash
curl http://localhost:8080/api/v1/analysis/1/feedback
```

### Sample Response

```json
{
  "id": 1,
  "content": "Islam promotes violence and terrorism",
  "sourceType": "SOCIAL_MEDIA",
  "platform": "Twitter",
  "violationConfirmed": true,
  "violationCategory": "TERRORISM_ASSOCIATION",
  "confidenceScore": 0.92,
  "analysis": {
    "explanation": "This statement incorrectly associates Islam with terrorism...",
    "counterArgument": "Islam explicitly condemns violence against innocents...",
    "quranReferences": [
      {
        "surah": "Al-Ma'idah",
        "verse": "32",
        "text": "Whoever kills a soul unless for a soul or for corruption [done] in the land - it is as if he had slain mankind entirely..."
      }
    ],
    "hadithReferences": [
      {
        "narrator": "Abu Hurairah",
        "text": "The Prophet said, 'Whoever kills a person under covenant will not smell the fragrance of Paradise.'"
      }
    ]
  },
  "feedbackStats": {
    "likes": 15,
    "dislikes": 2
  }
}
```

## Database Schema

The application uses JPA to auto-create tables. Key tables:
- `content_items`: Collected content
- `content_analysis`: AI analysis results
- `quran_references`: Quran verse references
- `hadith_references`: Hadith references
- `user_feedback`: User feedback
- `feedback_tracking`: Anti-spam tracking

## Troubleshooting

### Common Issues

**Application won't start - Database connection error:**
```bash
# Ensure PostgreSQL is running
docker ps | grep postgres-h8sense

# Check database logs
docker logs postgres-h8sense

# Restart database if needed
docker restart postgres-h8sense
```

**Port 8080 already in use:**
```bash
# Run on different port
java -jar target/islamophobia-detector-1.0.0-SNAPSHOT.jar --server.port=8081
```

**OpenAI API errors:**
```bash
# Verify your API key is set
echo $OPENAI_API_KEY

# Test API key with curl
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

**No sample data appearing:**
- Sample data loads only on first startup when database is empty
- Delete database and restart to reload samples:
```bash
docker rm -f postgres-h8sense
# Then follow Quick Start steps again
```

### Logs

View application logs:
```bash
# Console output (when running in foreground)
# Or check logs/application.log file

# Enable debug logging
export LOG_LEVEL_APP=DEBUG
java -jar target/islamophobia-detector-1.0.0-SNAPSHOT.jar
```

## Contributing

### Metrics (Prometheus)
- HTTP request counts and latencies
- AI analysis metrics
- Feedback statistics
- JVM metrics

### Tracing (Jaeger)
- Distributed tracing across services
- Request flow visualization
- Performance bottleneck identification

### Logging
- Structured JSON logging
- File and console output
- Configurable log levels

## Security Considerations

- CORS configured for cross-origin requests
- CSRF disabled for stateless API
- IP-based rate limiting for feedback
- Device fingerprinting for duplicate detection
- Non-root user in Docker container

## Future Enhancements

- [ ] Content fetcher modules for specific platforms (Twitter API, RSS feeds)
- [ ] Custom fine-tuned Islamic LLM model
- [ ] Multi-language support
- [ ] Advanced device fingerprinting
- [ ] User authentication and authorization
- [ ] Admin dashboard for content moderation
- [ ] Real-time notifications
- [ ] Export functionality (PDF reports)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

MIT License - See LICENSE file for details

## Disclaimer

This tool is designed to promote understanding and provide educational responses to Islamophobic content. It should be used responsibly and in accordance with platform terms of service and local laws.
