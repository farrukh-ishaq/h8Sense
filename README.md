# Islamophobia Content Detector

An AI-powered microservice application that automatically detects hate speech and Islamophobic content from online sources (newspapers, social media platforms, etc.) and analyzes it using an LLM trained on Quran and Hadith to provide informed counter-arguments and references.

## Features

### Core Capabilities
- **Automated Content Monitoring**: Fetches content from newspapers, social media, forums, and blogs
- **AI-Powered Analysis**: Uses LLM (GPT-4) with Islamic knowledge base to detect Islamophobia
- **Scholarly Response Generation**: Provides counter-arguments with authentic Quran and Hadith references
- **Violation Metadata Tracking**: Records when, what, by whom violations were detected
- **User Feedback System**: Like/dislike functionality with IP/device fingerprinting to prevent spam
- **Real-time Feed**: Paginated feed of confirmed violations with full analysis

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

### Local Development

1. **Clone the repository**
```bash
git clone <repository-url>
cd islamophobia-detector
```

2. **Set environment variables**
```bash
export OPENAI_API_KEY=your-api-key-here
export DATABASE_URL=jdbc:postgresql://localhost:5432/islamophobia_detector
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
```

3. **Run with Docker Compose**
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

4. **Access the services**
- Application: http://localhost:8080
- Grafana: http://localhost:3000 (admin/admin123)
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686

### Manual Build & Run

```bash
# Build
mvn clean package -DskipTests

# Run with dev profile
java -jar target/islamophobia-detector-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

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

## Database Schema

The application uses JPA to auto-create tables. Key tables:
- `content_items`: Collected content
- `content_analysis`: AI analysis results
- `quran_references`: Quran verse references
- `hadith_references`: Hadith references
- `user_feedback`: User feedback
- `feedback_tracking`: Anti-spam tracking

## Observability

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
