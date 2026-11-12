# 2026-11-12 09:39:08 UTC — Transactional outbox on Postgres (no broker)

Spun off from `20261111-1831-interview-showcase-skill-gaps.md` (gap #6, Step 7).

## TL;DR

**Why:** Async messaging and the outbox pattern are a standard backend / system-design interview topic, and shop has neither. A broker (Kafka, RabbitMQ) would add infrastructure that every cloning student pays for, so the pattern has to be shown without one.
**End result:** Placing an order writes an `OrderPlaced` event to an `outbox` table in the same Postgres transaction as the order. An in-process relay polls the table, delivers each event at least once to an in-process handler, and marks it sent. Duplicate deliveries are harmless because the handler is idempotent. This lands in Java, .NET and TypeScript, in the monolith and the multitier backends, with tests that prove the atomic write and the at-least-once delivery.
**Unchanged:** no broker, no new container, no paid service: the default clone and pipeline stay $0 and zero-infra. Place-order behaviour, its API and the existing system tests stay the same. The relay can be switched off by configuration.

## ▶ Next executable step (resume here)

Refine first: run `/refine-plan` on this file to settle the open questions below (each has a recommendation), then start Step 1.

## Steps

- [ ] Step 1: Decide the event and its consumer. Recommendation: `OrderPlaced { orderNumber, sku, quantity, totalPrice, placedAt }`, consumed by an in-process handler that records an order-placed audit/notification row (observable in tests, no external system).
- [ ] Step 2: Flyway migration in `system/db/migrations/` (forward-only, expand-contract): an `outbox` table (`id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload jsonb`, `created_at`, `sent_at`, `attempts`), plus an index on unsent rows, and the consumer's own table with a unique key on the event id (idempotent handling).
- [ ] Step 3: Java (monolith + multitier backend + clean variant where it fits): write the outbox row in the place-order transaction; add a scheduled relay (`SELECT … FOR UPDATE SKIP LOCKED`, batch, mark sent, bounded retries with backoff); unit + component tests for the atomic write (order rolled back ⇒ no event) and the relay (crash before mark-sent ⇒ redelivered, handled once).
- [ ] Step 4: .NET twin (EF Core transaction + `BackgroundService` relay), same tests.
- [ ] Step 5: TypeScript twin (Next.js monolith: `pg` transaction + interval relay started from instrumentation; NestJS backend: `@nestjs/schedule` or an interval provider), same tests.
- [ ] Step 6: Observability hook: a span per relayed event and an `outbox_pending` gauge, so the existing opt-in `otel-lgtm` profile shows the backlog.
- [ ] Step 7: `docs/tour.md` skills-map row (Messaging / transactional outbox) and a short how-to.

## Open questions

- **Where the relay runs in multitier:** inside each backend process (recommended: zero infra, matches the monolith) or a separate worker container (more realistic, but a new service in every compose file).
- **Polling vs `LISTEN/NOTIFY`:** start with polling (recommended: simplest, works with Cloud Run's request-scoped CPU when min instances = 0 is not a concern); `LISTEN/NOTIFY` as a later optimisation.
- **System-test coverage:** an acceptance criterion on UC-001 ("placing an order records an order-placed notification") vs component tests only. Recommendation: component tests only, since the event has no user-visible effect yet.

## Constraints

- Opt-in, $0 and zero-infra by default; no broker, no new container on the default path.
- Every language change lands in Java, .NET and TypeScript, latest and legacy where applicable.
- Schema changes follow the forward-only, expand-contract doctrine in `CONTRIBUTING.md`.
