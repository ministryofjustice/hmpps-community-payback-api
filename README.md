# HMPPS Community Payback API

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-community-payback-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-community-payback-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-community-payback-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://community-payback-api-dev.hmpps.service.justice.gov.uk/v3/api-docs)

HMPPS Community Payback API is a Kotlin/Spring Boot service that exposes APIs to support Community Payback UIs. It follows HMPPS platform conventions for security, observability, CI/CD, and deployment to the MoJ Cloud Platform via Helm.

- Language/Runtime: Kotlin 2 (Java 21), Spring Boot
- Build: Gradle (Wrapper included)
- Packaging: Docker
- Docs: OpenAPI/Swagger UI
- Security: OAuth2 Resource Server (JWTs from HMPPS Auth)
- Observability: Application Insights, Sentry

## Getting started (local development)

cp-stack is used to run the stack locally, either using the latest docker images or running spring boot directly via gradle. For more information see the [cp-stack README](tools/cp-stack/README.md)

If you want to run spring boot directly you will need:

- JDK 21
- curl (optional, for health checks)

Once the API is started you can run a test scenario against it using the [Intellij http-client test scripts](tools/http-client/README.md).

### Configuration

Configuration is primarily via Spring properties which can be overwritten via environment variables in Helm or cp-stack

[cp-stack](tools/cp-stack/README.md) provides a working default configuration, see the [README](tools/cp-stack/README.md) for information on how this works

### API docs

The latest version of the API docs can be retrieved from a local deployment, or from the dev deployment:

- OpenAPI spec: https://community-payback-api-dev.hmpps.service.justice.gov.uk/v3/api-docs
- Swagger UI: https://community-payback-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html

### Security

This service is an OAuth2 Resource Server. Most non-actuator endpoints require a valid Bearer token issued by HMPPS Auth and appropriate roles.

To get a token for local calls, use `tools/scripts/fetch_token.sh`

## Build, Test, and Quality

- Build: `./gradlew clean build`
- Run tests: `./gradlew test`
- Static code analysis/lint (ktlint & Detekt): `./gradlew ktlintFormat && ./gradlew detekt`
- Code coverage (JaCoCo): generated after tests (`build/reports/jacoco/test/html/index.html`)
  - Coverage gate: 80% lines (see `build.gradle.kts`)

### Building & running using docker locally

This is helpful when checking new docker images/configuration

```
./gradlew clean assemble
cp build/libs/*.jar .
docker build --build-arg BUILD_NUMBER=$(date '+%Y-%m-%d') .
```

Then update cp-tools/compose.yml to use the locally built docker image by using the image's sha256 ID and comment out the pull_policy e.g.

```
api:
  image: sha256:fd2c4f4fa2e448347339c3869ad7b8241535ecad4b6c86e0593405c7cee0b498
  container_name: cp-stack-api
  # if you want to run a locally built docker image, comment out the line below
  #pull_policy: always
```

Then start cp-stack, being sure to NOT include `--local-api` (i.e. use docker api image)
