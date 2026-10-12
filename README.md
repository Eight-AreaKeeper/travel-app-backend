# travel-app-backend

REST API for **TravelPass**, a travel web app where people share travel notes and get a personalized,
day-by-day itinerary generated from a short Q&A.

- Travel notes & guides: publish notes with photos, tags and a day-by-day route; search and filter them; comment, like and favorite.
- Trip customization: answer a few questions (destinations, dates, budget, travelers, style), even before logging in.
- Itinerary engine: generates a multi-day, multi-destination plan asynchronously (RabbitMQ). It uses Google Gemini when an API key is configured and falls back to a built-in rule engine otherwise. Users can swap, edit or remove items, regenerate with new constraints (edited items are kept), and export the plan as a PDF.

The web client lives in a separate repo: **[travel-app-frontend](https://github.com/Eight-AreaKeeper/travel-app-frontend)** (React + Vite).

## Tech stack

| Area | Choice |
|---|---|
| Runtime | Java 17, Spring Boot 3.5 |
| Web / security | Spring Web, Spring Security, stateless JWT (access + refresh tokens) |
| Data | MySQL 8 via MyBatis-Plus |
| Cache | Redis (SMS codes, anonymous customize-session tokens) |
| Messaging | RabbitMQ (async itinerary generation) |
| AI | Spring AI + Google Gemini (optional) |
| Maps | AMap (Gaode) Web Service API for POI backfill (optional) |
| PDF | OpenPDF, with the bundled Noto Sans SC font for CJK text |
| API docs | springdoc-openapi + knife4j UI |

## Project structure

```
db/
├── schema.sql          Full MySQL schema (run once on a fresh database)
├── seed_data.sql       Destinations, POIs and the customize Q&A questions
└── migrations/         Upgrade scripts for databases created from an older schema
src/main/java/com/travelapp
├── user         Registration (SMS code), login, token refresh, profile
├── content      Destinations, travel notes, comments, likes, favorites
├── customize    Q&A questions, customize sessions and answers
├── itinerary    Itinerary engine (rule engine + Gemini), RabbitMQ consumer, adjustments, PDF export
├── map          AMap POI lookups
├── file         Image upload (local disk)
├── security     JWT issuing/verification, auth filter, current-user lookup
├── common       Response wrapper, error codes, global exception handler
└── config       Security/CORS, MyBatis-Plus, OpenAPI configuration
src/main/resources
├── application.yml                Shared configuration (no secrets)
├── application-dev.yml.example    Template for your local secrets
└── db/seed-test-data.sql          Optional sample users / notes / comments
```

Each module is split into `controller / service / mapper / entity / dto`. Data access uses MyBatis-Plus mappers;
simple queries are built with `LambdaQueryWrapper` in the service layer.

## Getting started

### 1. Prerequisites

- **JDK 17.** Newer JDKs (23+) disable implicit annotation processing, so Lombok does not run and the build fails with
  hundreds of "cannot find symbol" errors. Use JDK 17 (or 21), e.g. point `JAVA_HOME` at it.
- Maven 3.9+
- MySQL 8, Redis and RabbitMQ — either via Docker (below) or installed locally.

### 2. Start MySQL, Redis and RabbitMQ

```bash
docker compose up -d
```

- MySQL on `localhost:3306`. On first start the container creates the `travel_app` database and runs
  `db/schema.sql` and `db/seed_data.sql` automatically (root password: see `docker-compose.yml`).
- Redis on `localhost:6379`.
- RabbitMQ on `localhost:5672`, management UI at <http://localhost:15672> (guest/guest).

Using your own MySQL instead? Create the schema and seed data manually:

```bash
mysql -uroot -p < db/schema.sql      # WARNING: drops and recreates all tables
mysql -uroot -p < db/seed_data.sql
mysql -uroot -p < src/main/resources/db/seed-test-data.sql   # optional sample notes
```

If your database was created from an older schema, apply the scripts in `db/migrations/` in order instead of re-running `schema.sql`.

### 3. Configure

Copy the template and fill in your local values:

```bash
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
```

| Key (in `application-dev.yml`) | Purpose |
|---|---|
| `app.datasource.*` | MySQL host, port, database, username, password |
| `app.redis.*` | Redis host, port, database index |
| `app.rabbitmq.*` | RabbitMQ host, port, credentials |
| `app.ai.gemini.api-key` | Optional. Gemini API key; leave blank to use the rule engine only |
| `app.map.amap.key` | Optional. AMap Web Service key, used to backfill POIs for destinations with little local data |

`application-dev.yml` is gitignored; never commit real credentials. Other settings (JWT lifetimes, upload directory,
Gemini model, AMap thresholds) live in `application.yml`. Change `app.jwt.secret` before deploying anywhere public.

### 4. Run

```bash
mvn spring-boot:run
```

Or run `TravelAppApplication` from your IDE. Then:

- Health check: <http://localhost:8080/actuator/health>
- API docs (knife4j): <http://localhost:8080/doc.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Build a jar with `mvn -DskipTests package` (output in `target/`).

### 5. Start the frontend

Clone [travel-app-frontend](https://github.com/Eight-AreaKeeper/travel-app-frontend) and run `npm install && npm run dev`.
Its Vite dev server (<http://localhost:5173>) proxies `/api` and `/uploads` to this backend on port 8080.

## API conventions

- Base path: `/api/v1`. All responses use one envelope: `{ "code": 0, "message": "success", "data": ... }`.
- `code` is `0` on success. Business errors are returned with **HTTP 200** and a non-zero `code`; clients must check `code`, not just the HTTP status.
- Authentication: `Authorization: Bearer <accessToken>`. A missing, invalid or expired token returns **HTTP 401** with `code: 40100`; use `POST /api/v1/auth/refresh` with the refresh token to get a new access token. Access denied returns HTTP 403 with `code: 40300`.
- Anonymous customize sessions are identified by the `X-Session-Token` header returned when the session is created; after login, `POST /api/v1/customize/sessions/{id}/bind` attaches the session to the account.

| Code | Meaning |
|---|---|
| 40001 | Parameter validation failed |
| 40100 | Not logged in or token expired |
| 40300 | Forbidden |
| 40400 | Resource not found |
| 40900 | Conflict |
| 42900 | Too many requests (e.g. SMS code requested again within 60 s) |
| 50000 | Internal server error |
| 50300 | A third-party service is unavailable |

The full, always up-to-date endpoint list is in the knife4j docs at `/doc.html`.

## Notes and limitations

- **SMS codes are not actually sent.** `SmsCodeServiceImpl` writes the code to the backend log
  (`phone=... code=...`); read it from there when registering locally. Plug in a real SMS provider before production.
- **Itinerary generation** first tries Gemini (if `app.ai.gemini.enabled` is true and a key is set) and validates the
  result against real POIs; on any failure it falls back to the rule engine, so the app works without an API key.
- **POI coverage**: the seed data only has enough POIs for 大理 (Dali) and 成都 (Chengdu). Other destinations work as
  free text but get sparse plans unless an AMap key is configured to backfill POIs.
- **Uploads** are stored on local disk under `uploads/` and served at `/uploads/**`.
- Place names in the seed data are real Chinese locations and are intentionally kept in Chinese.
