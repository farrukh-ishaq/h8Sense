# Pull Request: Islamophobia Content Detector - AI-Powered Hate Speech Analysis System

## 🎯 Overview
This PR introduces a comprehensive AI-powered microservice application that automatically detects hate speech and Islamophobic content from online sources (newspapers, social media platforms, forums, blogs) and analyzes it using an LLM trained on Quran and Hadith to provide informed counter-arguments and authentic Islamic references.

## ✨ Key Features

### Core Capabilities
- **Automated Content Monitoring**: Fetches content from newspapers, social media, forums, and blogs
- **AI-Powered Analysis**: Uses LLM (GPT-4 via Spring AI) with Islamic knowledge base to detect Islamophobia
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

## 📁 Project Structure

```
islamophobia-detector/
├── src/main/java/com/islamophobia/detector/
│   ├── config/              # Security and app configuration
│   ├── controller/          # REST API endpoints
│   ├── model/
│   │   ├── entity/         # JPA entities
│   │   ├── dto/            # Data transfer objects
│   │   └── enums/          # Enumerations
│   ├── repository/         # Spring Data repositories
│   ├── service/            # Business logic services
│   ├── ai/                 # AI/LLM integration
│   └── observer/           # Content fetching observers
├── docker/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── prometheus.yml
│   └── grafana/
└── pom.xml
```

## 🗄️ Entity Model

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

## 🔌 API Endpoints

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

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- OpenAI API key

### Quick Start with Docker
```bash
cd docker
docker-compose up -d
```

This starts:
- PostgreSQL database (port 5432)
- Application (port 8080)
- Prometheus (port 9090)
- Grafana (port 3000) - admin/admin123
- Jaeger (port 16686)

### Environment Variables
```bash
export OPENAI_API_KEY=your-api-key-here
export DATABASE_URL=jdbc:postgresql://localhost:5432/islamophobia_detector
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
```

## 📊 Observability

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

## 🔒 Security Considerations

- CORS configured for cross-origin requests
- CSRF disabled for stateless API
- IP-based rate limiting for feedback
- Device fingerprinting for duplicate detection
- Non-root user in Docker container

## 📝 Files Changed

- **28 files changed**
- **2,020 insertions(+)**
- **203 deletions(-)**

### New Files
- Complete Spring Boot application structure
- Docker configuration with observability stack
- Comprehensive README documentation
- Entity layer with proper JPA relationships
- REST API controllers with validation
- Service layer with business logic
- AI integration with Spring AI

## 🧪 Testing

Run tests with:
```bash
mvn clean test
```

Build with:
```bash
mvn clean package -DskipTests
```

## 📋 Checklist

- [x] Code follows project conventions
- [x] Entity layer properly designed with JPA relationships
- [x] API design follows REST best practices
- [x] Observability configured (metrics, tracing, logging)
- [x] Docker setup complete with all services
- [x] Documentation comprehensive
- [x] Security considerations addressed
- [x] Environment configuration documented

## 🔮 Future Enhancements

- Content fetcher modules for specific platforms (Twitter API, RSS feeds)
- Custom fine-tuned Islamic LLM model
- Multi-language support
- Advanced device fingerprinting
- User authentication and authorization
- Admin dashboard for content moderation
- Real-time notifications
- Export functionality (PDF reports)

## ⚠️ Disclaimer

This tool is designed to promote understanding and provide educational responses to Islamophobic content. It should be used responsibly and in accordance with platform terms of service and local laws.

---

**Reviewers**: Please review the entity architecture, API design, and security configurations. Special attention should be paid to the anti-spam measures in the feedback system and the AI integration patterns.
