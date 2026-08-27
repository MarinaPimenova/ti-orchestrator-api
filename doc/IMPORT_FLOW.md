# Questions File Import Microservices System

An asynchronous, event-driven microservice system designed to upload, validate, parse, and store questions from CSV/XLSX files. Built with **Java 21**, **Spring Boot 4.1.0**, **RabbitMQ**, **Server-Sent Events (SSE)**, **PostgreSQL**, and **Micrometer Observation / Tracing**.

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


| Queue                     | Exchange    | Routing Key        | Producer      | Consumer      |
|:--------------------------|:------------|:-------------------|:--------------|:--------------|
| `upload-worker.import`    | `ti.upload` | `upload.requested` | Orchestrator  | Upload Worker |
| `upload-worker.completed` | `ti.upload` | `upload.completed` | Upload Worker | Orchestrator  |
| `upload-worker.fail`      | `ti.upload` | `upload.failed`    | Upload Worker | Orchestrator  |

> **Note:** The applications do not declare or generate queues dynamically at runtime. Queues, exchanges, and bindings are pre-created via RabbitMQ startup definitions (`definitions.json`).

---

## 📁 Project Structure

```text
├── ti-event-model (Shared Event Library)
│   └── event/
│       ├── ImportRequestedEvent.java
│       ├── ImportCompletedEvent.java
│       └── ImportFailedEvent.java
│
├── ti-orchestrator-api
│   ├── controller/
│   │   ├── ImportController.java
│   │   └── ImportSseController.java
│   ├── service/
│   │   ├── ImportService.java
│   │   ├── SseService.java
│   │   └── FileStorageService.java
│   ├── rabbit/
│   │   ├── RabbitPublisher.java
│   │   ├── ImportCompletedListener.java
│   │   └── ImportFailedListener.java
│   └── config/
│       └── RabbitConfig.java
│
└── ti-import-worker
    ├── rabbit/
    │   ├── ImportRequestedListener.java
    │   └── RabbitPublisher.java
    ├── service/
    │   └── WorkerImportService.java
    └── event/


```

### 1. Event Model (`ti-event-model`)

Placed inside a shared library (`ti-event-model`). The `importId` serves as the primary **Correlation ID** across HTTP, RabbitMQ message headers, logs, metrics, and SSE connection tracks.

```java
public record ImportRequestedEvent(
    UUID importId,
    String originalFilename,
    String storedFilename,
    String path,
    Instant createdAt,
    String traceId
) {}

public record ImportCompletedEvent(
    UUID importId,
    int importedQuestions,
    Duration duration
) {}

public record ImportFailedEvent(
    UUID importId,
    String reason
) {}

```

---

### 💾 2. File Storage

Files are written to a configured storage folder outside of the compiled application executable JARs.

#### Configuration (`application.yml`)

```yaml
ti:
  storage:
    path: ./storage/import
```

#### Implementation (`FileStorageService.java`)

```java
@Service
public class FileStorageService {

    @Value("${ti.storage.path}")
    private Path root;

    public StoredFile store(MultipartFile file, UUID importId) throws IOException {
        Files.createDirectories(root);

        String filename = importId + "-" + file.getOriginalFilename();
        Path target = root.resolve(filename);

        file.transferTo(target);

        return new StoredFile(
                filename,
                target.toString()
        );
    }
}

```

#### Target Storage Directory Example

```text
storage/
└── import/
    ├── a13d-questions.csv
    └── b66a-java.xlsx

```

---

### 📡 3. Server-Sent Events (SSE) Flow

#### SSE Controller (`ImportSseController.java`)

```java
@RestController
@RequestMapping("/rest/v1/import/sse")
@RequiredArgsConstructor
public class ImportSseController {

    private final SseService sseService;

    @GetMapping("/{importId}")
    public SseEmitter subscribe(@PathVariable UUID importId) {
        return sseService.create(importId);
    }
}
```

#### SSE Service (`SseService.java`)

```java
@Service
public class SseService {

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter create(UUID id) {
        SseEmitter emitter = new SseEmitter(10 * 60_000L); // 10 minutes timeout

        emitters.put(id, emitter);

        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));

        return emitter;
    }

    public void complete(UUID id, Object body) {
        SseEmitter emitter = emitters.remove(id);

        if (emitter == null) return;

        try {
            emitter.send(body);
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
```

---

### 🚀 4. Import API Endpoint

The POST endpoint returns an immediate response with `STARTED` status while handling parsing asynchronously in the background.

```java
@RestController
@RequestMapping("/rest/v1/import")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping
    public ImportResponse upload(@RequestParam MultipartFile file) {
        UUID id = importService.upload(file);
        return new ImportResponse(id, "STARTED");
    }
}
```

#### `ImportService.java`

```java
@Service
@RequiredArgsConstructor
public class ImportService {

    private final FileStorageService storage;
    private final RabbitPublisher publisher;
    private final Tracer tracer;

    public UUID upload(MultipartFile file) throws IOException {
        UUID id = UUID.randomUUID();

        StoredFile stored = storage.store(file, id);

        publisher.publish(new ImportRequestedEvent(
                id,
                file.getOriginalFilename(),
                stored.filename(),
                stored.path(),
                Instant.now(),
                tracer.currentTraceContext().context().traceId()
        ));

        return id;
    }
}
```

---

### 📬 5. RabbitMQ Publisher Setup

#### Converter Configuration (`RabbitConfig.java`)

```java
@Configuration
public class RabbitConfig {

    @Bean
    MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}
```

#### Message Publisher (`RabbitPublisher.java`)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitPublisher {

    private final RabbitTemplate rabbit;

    public void publish(ImportRequestedEvent event) {
        rabbit.convertAndSend(
                "ti.import",
                "import.requested",
                event
        );

        log.info("Published ImportRequestedEvent {}", event.importId());
    }
}
```

---

### ⚙️ 6. Worker Listener & Processing Service

#### Listener (`ImportRequestedListener.java`)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportRequestedListener {

    private final WorkerImportService workerImportService;

    @RabbitListener(queues = "import-worker.import")
    public void receive(ImportRequestedEvent event) {
        log.info("Import started {}", event.importId());
        workerImportService.process(event);
    }
}
```

#### Worker Service (`WorkerImportService.java`)

```java
@Service
@RequiredArgsConstructor
public class WorkerImportService {

    private final FileLoader fileLoader;
    private final DataParser parser;
    private final QuestionService questionService;
    private final RabbitPublisher publisher;

    @Transactional
    public void process(ImportRequestedEvent event) {
        long start = System.nanoTime();

        try {
            MultipartFile file = fileLoader.load(event.path());

            List<QuestionRow> rows = parser.parse(file);

            List<Question> questions = questionService.generate(rows);

            int inserted = questionService.bulkInsert(questions);

            publisher.completed(
                    new ImportCompletedEvent(
                            event.importId(),
                            inserted,
                            Duration.ofNanos(System.nanoTime() - start)
                    ));

        } catch (Exception ex) {
            publisher.failed(
                    new ImportFailedEvent(
                            event.importId(),
                            ex.getMessage()
                    ));

            throw ex;
        }
    }
}
```

---

### 🏁 7. Completion Handlers (Orchestrator)

#### Success Listener (`ImportCompletedListener.java`)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportCompletedListener {

    private final MeterRegistry meterRegistry;
    private final SseService sse;

    @RabbitListener(queues = "import-worker.completed")
    public void completed(ImportCompletedEvent event) {
        meterRegistry.counter("ti.import.completed").increment();

        sse.complete(event.importId(), event);

        log.info("Import completed {}", event.importId());
    }
}

```

#### Failure Listener (`ImportFailedListener.java`)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ImportFailedListener {

    private final MeterRegistry meterRegistry;
    private final SseService sse;

    @RabbitListener(queues = "import-worker.fail")
    public void failed(ImportFailedEvent event) {
        meterRegistry.counter("ti.import.failed").increment();

        sse.complete(event.importId(), event);

        log.error("Import failed {} : {}", event.importId(), event.reason());
    }
}

```

---

### 📊 8. Observability & Prometheus Metrics

#### Business Metrics Service

```java
@Service
@RequiredArgsConstructor
public class ImportMetrics {

    private final MeterRegistry registry;

    public Counter started() {
        return registry.counter("ti.import.started");
    }

    public Counter completed() {
        return registry.counter("ti.import.completed");
    }

    public Counter failed() {
        return registry.counter("ti.import.failed");
    }

    public Timer timer() {
        return registry.timer("ti.import.duration");
    }
}

```

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

#### Manual Observation Wrap

```java
Observation.createNotStarted("ti.import.worker", observationRegistry)
           .observe(() -> process(event));

```

Every structured log output includes tracing metadata:

```text
2026-08-20 14:33:00.123 INFO [ti-import-worker,traceId=64a1f33,spanId=b821a3] : Import completed: importId=a13d-98bc, inserted=42
```

---

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



