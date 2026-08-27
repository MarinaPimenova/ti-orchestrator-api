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
---


## 💡 Architecture Overview

The system decouples file upload from heavy batch parsing and database insertion using RabbitMQ queues and Server-Sent Events (SSE) to deliver real-time progress updates back to the UI.
```
UI Client ──(1. POST CSV/XLSX)──> Orchestrator (ti-orchestrator-api)
│                                   │
│                                   ├──(2. Stores file on Disk)
├──(3. GET SSE Stream)──────────────┤
│                                   └──(4. Publishes ImportRequestedEvent)
│                                                   │
│                                                   ▼
│                                        RabbitMQ [import-worker.import]
│                                                   │
│                                                   ▼
│                                         Worker (ti-import-worker)
│                                                   │
│                                                   ├──(Parses & Bulk Save to PostgreSQL)
│                                                   │
│                                                   ├──(Success)──> RabbitMQ [import-worker.completed]
│                                                   └──(Failure)──> RabbitMQ [import-worker.fail]
│                                                                           │
└──────────────(5. SSE Result Event & Close Stream) <───────────────────────┘
```
---

## 🔄 Messaging & Queue Topology

Separate queues are used for requests, completions, and failures. This prevents workers from consuming their own processing completion events and makes queue metrics and monitoring simpler.

| Queue                     | Exchange    | Routing Key        | Producer      | Consumer      |
|:--------------------------|:------------|:-------------------|:--------------|:--------------|
| `import-worker.import`    | `ti.import` | `import.requested` | Orchestrator  | Import Worker |
| `import-worker.completed` | `ti.import` | `import.completed` | Import Worker | Orchestrator  |
| `import-worker.fail`      | `ti.import` | `import.failed`    | Import Worker | Orchestrator  |


| Queue                       | Exchange      | Routing Key          | Producer        | Consumer        |
|:----------------------------|:--------------|:---------------------|:----------------|:----------------|
| `upload-worker.upload`    | `ti.document` | `upload.requested` | Orchestrator    | Document Worker |
| `upload-worker.completed` | `ti.document` | `upload.completed` | Document Worker | Orchestrator    |
| `upload-worker.fail`      | `ti.document` | `upload.failed`    | Document Worker | Orchestrator    |

> **Note:** The applications do not declare or generate queues dynamically at runtime. Queues, exchanges, and bindings are pre-created via RabbitMQ startup definitions (`definitions.json`).


---
#### Metrics Summary

| Metric Name | Type | Description |
| --- | --- | --- |
| `ti.import.started` | Counter | Total imports initiated |
| `ti.import.completed` | Counter | Total successfully processed imports |
| `ti.import.failed` | Counter | Total failed imports |
| `ti.import.duration` | Timer | Processing duration histogram |

#### Useful PromQL Queries

* **Success Rate (5m):**
```promql
sum(rate(ti_import_completed_total[5m]))

```

* **Failure Rate (5m):**
```promql
sum(rate(ti_import_failed_total[5m]))

```

* **95th Percentile Import Latency:**
```promql
histogram_quantile(
  0.95,
  rate(ti_import_duration_seconds_bucket[5m])
)
```

---

### 🔍 9. Distributed Tracing & Logging

Spring Boot 4 automatically propagates trace context through RabbitMQ message headers when Micrometer Tracing is enabled.


### 🐳 10. RabbitMQ Topology & Docker Compose

#### `docker-compose.yml`

```yaml
version: '3.8'

services:
  rabbitmq:
    image: rabbitmq:4-management
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - ./rabbitmq/definitions.json:/etc/rabbitmq/definitions.json
      - ./rabbitmq/rabbitmq.conf:/etc/rabbitmq/rabbitmq.conf

```

#### `rabbitmq.conf`

```ini
management.load_definitions = /etc/rabbitmq/definitions.json
```

---

### ❌ 11. Failure Handling & Resilience

```text
CSV File (e.g. Invalid Format)
  │
  ▼
ti-import-worker
  │
  ├──> IllegalArgumentException
  │
  ├──> log.error("...")
  ├──> ti.import.failed Counter ++
  │
  └──> Publishes ImportFailedEvent
            │
            ▼
       RabbitMQ [import-worker.fail]
            │
            ▼
       ti-orchestrator-api
            │
            ▼
       SSE push message: { "importId": "...", "reason": "Invalid CSV header" }
            │
            ▼
       UI displays error notification & closes SSE connection
```
---

### 📌 Summary Features

* **Immediate Responsiveness**: HTTP request responds immediately with an `importId`.
* **Real-Time Delivery**: Clean push delivery via Server-Sent Events (SSE).
* **Decoupled Workflows**: Asynchronous worker isolation using RabbitMQ topic exchange topology.
* **End-to-End Correlation**: Unified tracing using `importId` and `traceId`.
* **Pre-defined Infrastructure**: Declarative topology via RabbitMQ Docker startup definitions.

