# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run with local profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=AlerteRappelApiApplicationTests

# Run a single test method
./mvnw test -Dtest=AlerteRappelApiApplicationTests#contextLoads
```

## Architecture

This is a **Spring Boot 4.0.5 / Java 25** REST API project. It uses virtual threads (`spring.threads.virtual.enabled=true`) and graceful shutdown.

Base package: `fr.graynaud.alerterappel.api`

### Domain

The API aggregates product recall data from two external sources into a unified schema:

- **Safety Gate / RAPEX** — EU product safety alerts (`/public/api/notification`)
- **RappelConso** — French government open data API

The unified recall object (`docs/schema.json`, documented in `docs/schema.md`) has these top-level sections:
- `_metadata` — import sources, dates, RappelConso GUID
- Root fields — `alert_number` (join key, always uppercased), `publication_date`, `risks`, `risk_description`
- `product` — name, brand, category, barcodes, batch numbers, etc.
- `commercialization` — origin/alert countries, reacting countries, distributors, sale period
- `measures` — measures list, company recall links, consumer actions, compensation terms
- `media` — photo URLs, recall sheet URL

**Key join key**: `alert_number` maps to `reference` (RAPEX) and `numero_fiche` (RappelConso). Always normalized to uppercase.

**URL construction**:
- RAPEX photo: `https://ec.europa.eu/safety-gate-alerts/public/api/notification/image/{photo.id}`
- RAPEX recall sheet: `https://ec.europa.eu/safety-gate-alerts/screen/webReport/alertDetail/{reference}`
- RappelConso photos: `liens_vers_les_images` split on `|`