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

## Code Style

- **`this.` on fields**: Always prefix instance field access with `this.` (e.g. `this.sourceName`, `this.dataPath`).
- **`this.` on methods**: Do NOT use `this.` when calling instance methods (e.g. `checkNewData()`, not `this.checkNewData()`).
- **No `var`**: Never use `var`. Always declare explicit types (e.g. `AlertProduct product = new AlertProduct(...)`, not `var product = new AlertProduct(...)`).

## Architecture

This is a **Spring Boot 4.0.5 / Java 25** REST API project. It uses virtual threads (`spring.threads.virtual.enabled=true`) and graceful shutdown.

Base package: `fr.graynaud.alerterappel.api`

### Profiles

- **default** — production settings (`application.properties`)
- **local** — local development (`application-local.properties`), port 9090
- **test** — test properties (`src/test/resources/application-test.properties`), used via `@ActiveProfiles("test")`

### JVM arguments

Lucene requires `--add-modules jdk.incubator.vector` for optimal vector performance. This is configured in:
- `pom.xml` — `spring-boot-maven-plugin` (`jvmArguments`) and `maven-surefire-plugin` (`argLine`)
- `.run/AlerteRappelApiApplication.run.xml` — IntelliJ run config (`VM_PARAMETERS`)

### Package structure

```
config/
  properties/        — @ConfigurationProperties classes
    SourceProperties         — abstract base (name)
    Explore21Properties      — abstract, adds baseUrl, dataset, cron + RestClient.Builder helper
    RappelConsoProperties    — prefix: source.rappelconso
    RapexProperties          — prefix: source.rapex (adds getBaseUrl, getPath, translationPath)
    DataProperties           — prefix: data (manages data directory + per-source JSON files)
  RestConfig                 — RestClient.Builder bean (JDK HttpClient, 10s connect / 30s read)
  SchedulingConfig           — TaskScheduler bean (SimpleAsyncTaskScheduler, virtual threads)

controller/
  publics/
    AlertController          — REST endpoints: suggest, search, latest, details by alert number, by barcode. Cache-Control on all endpoints.

service/
  alert/
    dto/             — Unified Alert schema (output): Alert, AlertProduct, AlertCommercialization, SearchSuggestion, etc.
    AlertService             — In-memory alert cache, barcode index, pagination, delegates search to Lucene
    AlertRepository          — JSON file persistence with FileLock
    AlertMerger              — Multi-source merge logic (RAPEX priority)
    AlertSearchIndex         — Lucene in-memory fulltext search (ByteBuffersDirectory)
    SuggestionIndex          — In-memory n-gram index for search suggestions (1–5 word n-grams)
  source/
    explore21/       — Abstract layer for Opendatasoft Explore 2.1 sources
      Explore21Source        — interface: getLastDate()
      Explore21Response<T>   — paginated response record {totalCount, results}
      Explore21Service<D>    — abstract service: FileLock, cron scheduling, rate-limit logging
    rappelconso/     — RappelConso source
      RappelConsoService     — extends Explore21Service<RappelConsoData>
      dto/                   — RappelConsoRappel, RappelConsoData, RappelConsoResponse
    rapex/           — Safety Gate / RAPEX source
      RapexService           — extends Explore21Service<RapexData>
      dto/                   — RapexNotification (full), RapexNotificationSummary, RapexData, RapexResponse, ...
```

### Fulltext search (Lucene)

`AlertSearchIndex` manages an in-memory Lucene index (`ByteBuffersDirectory`) rebuilt on each data reload. Indexed fields:
- `alertNumber`, `productName` (boosted x2), `brand`, `description`, `barcodes`, `batchNumbers`, `modelReferences`, `riskDescription`, `distributors`

Search uses per-token OR queries with fuzzy matching (edit distance 1 for tokens >= 3 chars, à la Elasticsearch AUTO fuzziness).

### Search suggestions

`SuggestionIndex` builds an in-memory index of n-grams (1 to 5 words) extracted from product names, brands, categories, and families. N-grams are normalized (lowercase, no diacritics) and deduplicated with occurrence counts. The index is rebuilt alongside the Lucene index on each data reload. Matching uses `startsWith` on normalized input, results sorted by frequency (most common first), limited to 8 suggestions.

### HTTP caching

`AlertController` sets `Cache-Control` headers on all endpoints:
- **suggest** — `max-age=1d, public` (static constant)
- **details / barcode** — `max-age=1d, public` (static constant)
- **latest** — dynamic `max-age` computed from source cron expressions (`cacheUntilNextCron()`), expires at the next cron execution across all sources

### Domain

The API aggregates product recall data from two external sources into a unified schema:

- **Safety Gate / RAPEX** — EU product safety alerts, listed via Opendatasoft (`source.rapex.*`), fetched individually via `https://ec.europa.eu/safety-gate-alerts/public/api/notification/{id}`
- **RappelConso** — French government open data API via Opendatasoft (`source.rappelconso.*`)

The unified recall object (`docs/schema.json`, documented in `docs/schema.md`) has these top-level sections:
- `metadata` — import sources, dates, RappelConso GUID
- Root fields — `alertNumber` (join key, always uppercased), `publicationDate`, `risks`, `riskDescription`
- `product` — name, brand, category, barcodes, batch numbers, etc.
- `commercialization` — origin/alert countries, reacting countries, distributors, sale period
- `measures` — measures list, company recall links, consumer actions, compensation terms
- `media` — photo URLs, recall sheet URL

**Key join key**: `alert_number` maps to `reference` (RAPEX) and `numero_fiche` (RappelConso). Always normalized to uppercase.

**URL construction**:
- RAPEX photo: `https://ec.europa.eu/safety-gate-alerts/public/api/notification/image/{photo.id}`
- RAPEX recall sheet: `https://ec.europa.eu/safety-gate-alerts/screen/webReport/alertDetail/{reference}`
- RappelConso photos: `liens_vers_les_images` split on `|`

### Data persistence

Each source has a JSON file under `${DATA_PATH}/source/<sanitized-name>.json` (e.g. `rappel_conso.json`). The file is created empty (`{}`) on first run. It stores the last processed date used to detect new records on the next cron run.

A sibling `.lock` file (`<file>.lock`) is used for inter-process locking via `java.nio.channels.FileLock` to prevent two server instances from processing the same source simultaneously.

### Scheduling

Each source defines its own cron expression via `source.<name>.cron`. The `TaskScheduler` bean (virtual threads) is injected into `Explore21Service` which registers `checkNewData()` programmatically at construction time via `taskScheduler.schedule(this::checkNewData, new CronTrigger(...))`.
