For the TI Knowledge Platform, I would recommend a consistent naming convention:

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

### Recommended queues

| Queue                              | Consumer              | Purpose                    |
| ---------------------------------- | --------------------- | -------------------------- |
| `ti-import-api.import`             | `ti-import-api`       | Process import requests    |
| `ti-import-api.import.retry`       | `ti-import-api`       | Retry failed imports       |
| `ti-import-api.import.fail`        | `ti-import-api`       | Permanently failed imports |
| `ti-import-api.import.duplicate`   | `ti-import-api`       | Duplicate import requests  |
| `ti-notification-api.notification` | `ti-notification-api` | Process notifications      |
| `ti-audit-api.audit`               | `ti-audit-api`        | Process audit events       |
| `ti-export-api.export`             | `ti-export-api`       | Process export requests    |

For your current import flow, the core naming would therefore be:

```text
ti-import-api.import
ti-import-api.import.retry
ti-import-api.import.fail
ti-import-api.import.duplicate
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
                    ti-import-api.import
                                   │
                              Import API
                                   │
                    ┌──────────────┴──────────────┐
                    │                             │
                 success                       failure
                    │                             │
                    ▼                             ▼
                                      ti-import-api.import.fail
```

### Recommended event names

I would also standardize event names independently from queue names:

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
ti-import-api.import
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

### One recommendation

I would **not** put `.dlx` on a queue that is actually a dead-letter queue unless it is genuinely the DLQ. If you're using a **dead-letter exchange**, keep the distinction explicit:

```text
Exchange:
ti-import-api.import.dlx

Queue:
ti-import-api.import.fail
```

This makes the terminology clear:

```text
ti-import-api.import
        │
        │ rejected / expired
        ▼
ti-import-api.import.dlx
        │
        ▼
ti-import-api.import.fail
```

For the project, I would adopt this convention globally:

```text
<consumer-service>.<workload>
<consumer-service>.<workload>.retry
<consumer-service>.<workload>.fail
<consumer-service>.<workload>.duplicate
```

It gives you an immediate answer to **"which service consumes this queue?"** just by looking at its name.
