For the TI Knowledge Platform, a consistent naming convention:

```text
<service>.<workload>[.<purpose>][.<suffix>]
```

Where:

* `<service>` — service/container responsible for consuming the queue
* `<workload>` — business operation
* `<purpose>` — optional distinction such as `events` or `commands`
* `.dlx` — dead-letter exchange/queue
* `.fail` — failed messages
* `.duplicate` — duplicate messages
* `.retry` — retry queue

### Queues

| Queue                              | Consumer              | Purpose                    |
| ---------------------------------- | --------------------- | -------------------------- |
| `ti-import-worker.import`             | `ti-import-worker`       | Process import requests    |
| `ti-import-worker.import.retry`       | `ti-import-worker`       | Retry failed imports       |
| `ti-import-worker.import.fail`        | `ti-import-worker`       | Permanently failed imports |
| `ti-import-worker.import.duplicate`   | `ti-import-worker`       | Duplicate import requests  |

For current import flow, the core naming would therefore be:

```text
ti-import-worker.import
ti-import-worker.import.retry
ti-import-worker.import.fail
ti-import-worker.import.duplicate
```

### Exchanges

I would keep exchanges separate from queues and use a similar convention:

```text
ti.import
ti.import.dlx
```

For example:

```text
                    ┌─────────────────────────────┐
                    │        ti.import             │
                    │      Topic Exchange          │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
                    ti-import-worker.import
                                   │
                              Import API
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
                 success                       failure
                    │                             │
                    ▼                             ▼
                                      ti-import-worker.import.fail
```

### Event names

Standardize event names independently from queue names:

```text
ImportRequestedEvent
ImportCompletedEvent
ImportFailedEvent
```

So the complete flow becomes:

```text
Orchestrator
     │
     │ ImportRequestedEvent
     ▼
ti.import
     │
     ▼
ti-import-worker.import
     │
     ▼
Import Service
     │
     │ ImportCompletedEvent
     ▼
ti.import
     │
     ├──────────────► Orchestrator
     │
     └──────────────► Audit Service
```

### Note

**not** put `.dlx` on a queue that is actually a dead-letter queue unless it is genuinely the DLQ. If you're using a **dead-letter exchange**, keep the distinction explicit:

```text
Exchange:
ti-import-worker.import.dlx

Queue:
ti-import-worker.import.fail
```

This makes the terminology clear:

```text
ti-import-worker.import
        │
        │ rejected / expired
        ▼
ti-import-worker.import.dlx
        │
        ▼
ti-import-worker.import.fail
```

For the project, adopt this convention globally:

```text
<consumer-service>.<workload>
<consumer-service>.<workload>.retry
<consumer-service>.<workload>.fail
<consumer-service>.<workload>.duplicate
```

It gives an immediate answer to **"which service consumes this queue?"** just by looking at its name.
