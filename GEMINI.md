# GEMINI.md

## Project Overview

This is a Spring Boot application written in Java 21, designed to be a "hidden gems" discovery and recommendation service for places in Korea. The standout feature is its use of MBTI personality types for personalized recommendations. The application leverages a modern tech stack, including vector search capabilities with `pgvector` for finding similar places.

The project is well-structured, following a layered architecture pattern, and includes comprehensive documentation for its API, batch processes, and overall structure.

### Core Technologies

*   **Backend:** Java 21, Spring Boot 3.2.0
*   **Database:** PostgreSQL with the `pgvector` extension, H2 for testing
*   **Data Migration:** Flyway
*   **Build Tool:** Gradle
*   **Authentication:** Spring Security with JWT
*   **API Documentation:** SpringDoc OpenAPI 3 (Swagger)
*   **Containerization:** Docker and Docker Compose

### Key Features

*   **MBTI-based Recommendations:** The core feature, providing personalized place suggestions based on user MBTI types.
*   **Vector Similarity Search:** Utilizes `pgvector` to find places with similar characteristics.
*   -**Data Ingestion:**  A Spring Batch job collects place data from the Naver and Google Places APIs.
*   **User Management:** Standard user account features, including email-based sign-up, JWT authentication, and profile management.
*   **RESTful API:** A well-documented REST API for interacting with the application.

## Building and Running

The project uses Gradle for building and can be run either locally or within a Docker container.

### Prerequisites

*   Java 21+
*   Docker & Docker Compose
*   Gradle 8.5+

### Environment Variables

Create a `.env` file in the project root and add the following, replacing the placeholder values:

```bash
# Database
DB_USERNAME=mohe_user
DB_PASSWORD=your_password

# Naver API (Required)
NAVER_CLIENT_ID=your_client_id
NAVER_CLIENT_SECRET=your_client_secret

# Google Places API (Optional)
GOOGLE_PLACES_API_KEY=your_api_key

# JWT Secret
JWT_SECRET=your_secret_key_minimum_64_characters
```

### Running with Docker (Recommended)

```bash
# Build and start the application and PostgreSQL database
docker-compose up --build

# To run in the background
docker-compose up -d

# To stop the services
docker-compose down
```

### Running Locally

```bash
# Start only the PostgreSQL database using Docker
docker-compose up postgres -d

# Run the Spring Boot application using Gradle
./gradlew bootRun
```

### Running Tests

```bash
# Run all tests (uses an H2 in-memory database)
./gradlew test
```

## Development Conventions

The project follows a standard layered architecture, which is clearly documented in `PROJECT_STRUCTURE.md`.

*   **Controller Layer:** Handles HTTP requests and responses. Should not contain business logic.
*   **Service Layer:** Contains the core business logic and orchestrates interactions between the repository and other services.
*   **Repository Layer:** Responsible for data access and interaction with the database.
*   **DTOs (Data Transfer Objects):** Used to transfer data between the layers and for API requests/responses. Entities should not be directly exposed in the API.
*   **Entities:** JPA entities that represent the database tables.

### API

After starting the application, the following endpoints are available:

*   **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
*   **OpenAPI Spec:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
*   **Health Check:** [http://localhost:8080/health](http://localhost:8080/health)
