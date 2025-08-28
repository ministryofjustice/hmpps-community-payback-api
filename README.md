# HMPPS Community Payback API

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-community-payback-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-community-payback-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-community-payback-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://community-payback-api-dev.hmpps.service.justice.gov.uk/v3/api-docs)

HMPPS Community Payback API is a Kotlin/Spring Boot service that exposes APIs to support Community Payback use cases. It follows HMPPS platform conventions for security, observability, CI/CD, and deployment to the MoJ Cloud Platform via Helm.

- Language/Runtime: Kotlin 2.2.10 (Java 21), Spring Boot
- Build: Gradle (Wrapper included)
- Packaging: Docker
- Docs: OpenAPI/Swagger UI
- Security: OAuth2 Resource Server (JWTs from HMPPS Auth)
- Observability: Application Insights, Sentry

## Environments

| Environment | Base URL |
|-------------|----------|
| Dev | https://community-payback-api-dev.hmpps.service.justice.gov.uk/ |

Important links:
- Dev OpenAPI spec: https://community-payback-api-dev.hmpps.service.justice.gov.uk/v3/api-docs
- Dev Swagger UI: https://community-payback-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html

## Getting started (local development)

Prerequisites:
- JDK 21
- Docker (for docker-compose)
- curl (optional, for health checks)

Clone the repo and set your environment variables. See `env.sample` for examples.

### Run with Gradle (local JVM)

- Start a local HMPPS Auth using docker-compose (optional but recommended to exercise secured endpoints):
```bash
docker compose up -d hmpps-auth
```
- Run the service with the local profile:
```bash
SPRING_PROFILES_ACTIVE=localdev ./gradlew bootRun
```
- The service will start on `http://localhost:8080`.

### Run everything with Docker Compose

- `docker compose up --build`
- This will start:
  - `hmpps-community-payback-api` on `http://localhost:8080`
  - `hmpps-auth` on `http://localhost:8090/auth`

### Configuration

Configuration is primarily via Spring properties and environment variables.

Key properties (see `src/main/resources/application.yml` and `application-localdev.yml`):
- `server.port` (default 8080)
- `hmpps-auth.url` (e.g. `http://localhost:8090/auth` for local dev)

Environment variables (examples in `env.sample`):
- `SERVER_PORT` — override server port
- `HMPPS_AUTH_URL` — base URL for HMPPS Auth (note: ensure it is a valid URL)
- `SPRING_PROFILES_ACTIVE` — use `localdev` for local JVM run with Docker

### API docs

- OpenAPI JSON: `GET /v3/api-docs`
- Swagger UI: `GET /swagger-ui/index.html`

### Security

This service is an OAuth2 Resource Server. Most non-actuator endpoints require a valid Bearer token issued by HMPPS Auth and appropriate roles.

Getting a token locally:
- For end-to-end manual testing, run the local `hmpps-auth` container and use its UI at `http://localhost:8090/auth` to authenticate and retrieve a token, or use client credentials if configured in your environment.

## Build, Test, and Quality

- Build: `./gradlew clean build`
- Run tests: `./gradlew test`
- Static code analysis/lint (ktlint & Detekt): `./gradlew ktlintFormat && ./gradlew detekt`
- Code coverage (JaCoCo): generated after tests (`build/reports/jacoco/test/html/index.html`)
  - Coverage gate: 80% lines (see `build.gradle.kts`)

## Running in Docker

Build image locally:
- `docker build -t hmpps-community-payback-api:local .`

Run container:
- `docker run -p 8080:8080 -e HMPPS_AUTH_URL=http://host.docker.internal:8090/auth hmpps-community-payback-api:local`

Or use docker-compose:
- `docker compose up --build`