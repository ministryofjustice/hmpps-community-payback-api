# HMPPS Community Payback API

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-community-payback-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-community-payback-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-community-payback-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://community-payback-api-dev.hmpps.service.justice.gov.uk/v3/api-docs)

HMPPS Community Payback API is a Kotlin/Spring Boot service that exposes APIs to support Community Payback use cases. It follows HMPPS platform conventions for security, observability, CI/CD, and deployment to the MoJ Cloud Platform via Helm.

- Language/Runtime: Kotlin 2 (Java 21), Spring Boot
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

[cp-stack](tools/cp-stack/README.md) is used to run the stack locally, either using the latest docker images or running spring boot directly via gradle

If you want to run spring boot directly you will need:

- JDK 21
- curl (optional, for health checks)

### Configuration

Configuration is primarily via Spring properties which can be overwritten via environment variables in Helm or cp-stack

[cp-stack](tools/cp-stack/README.md) provides a working default configuration, see the [README](tools/cp-stack/README.md) for information on how this works

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
