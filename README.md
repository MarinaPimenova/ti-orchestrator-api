# TI Orchestrator API

`ti-orchestrator-api` is a Spring Boot microservice of the **Training Internal (TI) Knowledge Platform**.

It is responsible for processing long-running jobs.

## Technology Stack

| Category          | Technology                  |
| ----------------- | --------------------------- |
| Language          | Java 21                     |
| Framework         | Spring Boot 4.0.7           |
| Build             | Gradle                      |
| API               | REST / Spring MVC           |
| Persistence       | Spring Data JPA / Hibernate |
| Security          | Spring Security / OAuth 2.0 |
| Identity Provider | Okta                        |
| API Documentation | Springdoc OpenAPI           |
| Metrics           | Micrometer / Prometheus     |
| Tracing           | Micrometer Tracing / Brave  |
| Containerization  | Docker                      |

## Prerequisites

* Java 21
* Docker
* PostgreSQL
* Okta OAuth 2.0 configuration

The project uses the Gradle Wrapper, so Gradle does not need to be installed separately.

Check Java:

```bash
java -version
```

## Build

Build the application:

```bash
./gradlew clean build
```

Build without tests:

```bash
./gradlew clean build -x test
```

The generated JAR is available in:

```text
build/libs/
```

## Run Locally

Start the application with:

```bash
./gradlew bootRun
```

The default port is:

```text
8082
```

Application URL:

```text
http://localhost:8082
```

## Configuration

Main configuration:

```text
src/main/resources/application.yaml
```

Local configuration:

```text
src/main/resources/application-local.yaml
```
