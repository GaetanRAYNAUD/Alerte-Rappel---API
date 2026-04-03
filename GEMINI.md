# GEMINI.md

## Project Overview
Alerte Rappel - API is a Spring Boot application designed to centralize product recall alerts from two primary sources:
1. **RappelConso**: French official government platform for product recalls.
2. **Safety Gate (RAPEX)**: European Union's rapid alert system for dangerous consumer products.

The API aggregates these disparate sources into a unified, consistent data model.

## Technology Stack
- **Language**: Java 25
- **Framework**: Spring Boot 4.0.5
- **Build Tool**: Maven

## Building and Running
The project uses the Maven Wrapper (`mvnw`).

- **Build**: `./mvnw clean package`
- **Run**: `./mvnw spring-boot:run`
- **Tests**: `./mvnw test`

## Project Structure
- `src/main/java/fr/graynaud/alerterappel/api/`: Main application source code.
    - `config/`: Configuration classes (CORS, REST, Scheduling).
    - `controller/`: REST endpoints.
    - `service/`: Business logic for fetching and parsing data from various sources (RappelConso, RAPEX, Explore21).
- `docs/`: API schemas and examples.

## Development Conventions
- Adhere to standard Spring Boot patterns.
- Data transfer between external APIs and internal models is handled via specialized DTO classes.
- New data sources should be implemented within `src/main/java/fr/graynaud/alerterappel/api/service/source/`.
