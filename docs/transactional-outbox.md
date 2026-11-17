# Transactional outbox (no broker)

How shop publishes an event reliably when the database commit and the "send" cannot share a transaction, without a message broker. **This is a Java-only worked example in [`system/multitier/backend-java`](../system/multitier/backend-java/).** The other five backends read the same schema and leave the tables empty.

## The problem it answers

After placing an order you want something else to happen: an audit entry, an email, a call to another system. Writing the order and then sending a message is a *dual write*. If the process dies between the two, the order exists but the message is never sent. If you send first and the commit fails, you have announced an order that does not exist.

The outbox removes the dual write. The event is written as a row in the same database transaction as the order, so it exists if and only if the order does. A separate relay reads the unsent rows, delivers them and marks them sent.

Shop's place-order does not need this yet: nothing happens after the commit. The consumer here (an audit row) exists to give the event somewhere to go. The pattern is shown because "how do you publish an event reliably?" is a standard system-design question, and this is the cheapest credible answer: the same Postgres, no broker, $0.

## How it works

```mermaid
sequenceDiagram
    participant API as POST /api/orders
    participant DB as Postgres
    participant Relay as OutboxRelay (every 2s)
    participant Handler as OrderPlacedAuditHandler
    API->>DB: BEGIN; INSERT orders; INSERT outbox (OrderPlaced); COMMIT
    Relay->>DB: BEGIN; SELECT ... FOR UPDATE SKIP LOCKED
    Relay->>Handler: handle(event)
    Handler->>DB: INSERT order_placed_audit ... ON CONFLICT (event_id) DO NOTHING (own transaction)
    Relay->>DB: UPDATE outbox SET sent_at = now(); COMMIT
```

| Piece | Where | What it does |
| --- | --- | --- |
| Schema | [`V20261117150000__add_outbox_and_order_placed_audit.sql`](../system/db/migrations/V20261117150000__add_outbox_and_order_placed_audit.sql) | `outbox` (event id, aggregate, type, `jsonb` payload, `sent_at`, `attempts`, `next_attempt_at`, `last_error`, partial index on unsent rows) and the consumer's `order_placed_audit`, keyed on the event id |
| Write | [`OrderService.placeOrder`](../system/multitier/backend-java/src/main/java/com/mycompany/myshop/backend/core/services/OrderService.java), [`OutboxStore.append`](../system/multitier/backend-java/src/main/java/com/mycompany/myshop/backend/core/outbox/OutboxStore.java) | Appends `OrderPlaced { orderNumber, sku, quantity, totalPrice, placedAt }` inside the order's transaction (`Propagation.MANDATORY`, so it cannot run without one) |
| Relay | [`OutboxRelay`](../system/multitier/backend-java/src/main/java/com/mycompany/myshop/backend/core/outbox/OutboxRelay.java), [`OutboxRelayScheduler`](../system/multitier/backend-java/src/main/java/com/mycompany/myshop/backend/core/outbox/OutboxRelayScheduler.java) | Polls with a fixed delay, claims a batch with `FOR UPDATE SKIP LOCKED` (several instances can poll at once), delivers, marks sent. A failed delivery gets `attempts + 1` and a backed-off `next_attempt_at`; after `max-attempts` the row stays unsent for an operator to inspect |
| Consumer | [`OrderPlacedAuditHandler`](../system/multitier/backend-java/src/main/java/com/mycompany/myshop/backend/core/outbox/OrderPlacedAuditHandler.java) | Runs in its own transaction, like a consumer across a broker would. The insert is a no-op on a repeated event id, so redelivery is harmless |
| Observability | [`OutboxConfig`](../system/multitier/backend-java/src/main/java/com/mycompany/myshop/backend/config/OutboxConfig.java), `OutboxRelay` | An `outbox.relay` observation per delivery (a span plus a timer tagged `event.type` and `outcome`) and an `outbox.pending` gauge (`outbox_pending` in Grafana) under the opt-in [`observability` profile](observability.md) |

### Delivery is at least once, so handling must be idempotent

If the relay dies after the handler committed but before it marked the row sent, the row is still unsent and the next poll delivers it again. That is why the consumer deduplicates on the event id. Exactly-once delivery is not on offer; "at least once plus an idempotent consumer" is how you get the same effect.

## Configuration

All under `outbox.relay` (see [`OutboxRelaySettings`](../system/multitier/backend-java/src/main/java/com/mycompany/myshop/backend/core/outbox/OutboxRelaySettings.java)), with defaults so nothing needs to be set:

| Property | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | `false` (env `OUTBOX_RELAY_ENABLED=false`) removes the scheduler. Orders still write events; they wait unsent and `outbox_pending` grows |
| `poll-interval` | `2s` | Fixed delay between polls |
| `batch-size` | `50` | Rows claimed per poll |
| `max-attempts` | `5` | Failed deliveries before the relay gives up on an event |
| `initial-backoff`, `backoff-multiplier` | `1s`, `2.0` | Retry delay is `initial-backoff * multiplier^(attempts - 1)`: 1s, 2s, 4s, 8s |

## Tests

- Unit: [`OutboxRelayTest`](../system/multitier/backend-java/src/test/java/com/mycompany/myshop/backend/core/outbox/OutboxRelayTest.java) (delivery order, failure and backoff, missing handler, one observation per delivery), [`OutboxRelaySchedulerTest`](../system/multitier/backend-java/src/test/java/com/mycompany/myshop/backend/core/outbox/OutboxRelaySchedulerTest.java) (defaults, off switch), [`OrderServiceOutboxTest`](../system/multitier/backend-java/src/test/java/com/mycompany/myshop/backend/core/services/OrderServiceOutboxTest.java).
- Component, against real Postgres: [`TransactionalOutboxComponentTest`](../system/multitier/backend-java/src/componentTest/java/com/mycompany/myshop/backend/component/outbox/TransactionalOutboxComponentTest.java). It shows that a rolled-back place-order leaves neither the order nor the event, that the relay delivers and marks the event sent, that a delivery whose acknowledgement was lost is redelivered and handled once, and that the gauge tracks the backlog. Run it with `./gradlew componentTest` in `system/multitier/backend-java` (needs Docker).

The system tests are unchanged: the event has no user-visible effect.

## Caveats

- **Cloud Run.** Prod runs with `--min-instances 0` and request-based CPU, so an in-process relay only gets CPU while a request is in flight. Events can wait until the next request, or until the instance wakes after scaling to zero. That is fine for an audit row and not fine for anything with a deadline. The fixes are an always-on CPU instance, a scheduled job that runs the relay, or `LISTEN/NOTIFY`.
- **Retention.** Sent rows are kept. A real system purges them after a while, or partitions the table by day.
- **Ordering.** Events are claimed oldest first, but a failed event is retried later while newer ones go ahead. Consumers must not rely on strict ordering.
- **Polling vs `LISTEN/NOTIFY`.** Polling is the simplest thing that works. `LISTEN/NOTIFY` would cut latency, with polling kept as the safety net.
