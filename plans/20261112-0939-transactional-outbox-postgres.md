# 2026-11-12 09:39:08 UTC — Transactional outbox on Postgres (no broker)

Spun off from `20261111-1831-interview-showcase-skill-gaps.md` (gap #6, Step 7).

## TL;DR

**Why:** Async messaging and the outbox pattern are a standard backend / system-design interview topic, and shop has neither. A broker (Kafka, RabbitMQ) would add infrastructure that every cloning student pays for, so the pattern has to be shown without one.
**End result:** Placing an order writes an `OrderPlaced` event to an `outbox` table in the same Postgres transaction as the order. An in-process relay polls the table, delivers each event at least once to an in-process handler, and marks it sent. Duplicate deliveries are harmless because the handler is idempotent. This lands in Java, .NET and TypeScript, in the monolith and the multitier backends, with tests that prove the atomic write and the at-least-once delivery.
**Unchanged:** no broker, no new container, no paid service: the default clone and pipeline stay $0 and zero-infra. Place-order behaviour, its API and the existing system tests stay the same. The relay can be switched off by configuration.

## Why we need it (confirmed by the user 2026-11-17: build it, Java-only)

The user asked "why do we need it" at G0 (2026-11-16), so here is the honest case. **For the product, we don't need it today.** Place-order reads from the ERP, tax and clock gateways and then writes one order row in one transaction (e.g. `system/multitier/backend-java/.../core/services/OrderService.java`). Nothing happens after the commit that could get lost, so shop has no dual-write problem for an outbox to solve. Step 1 even has to invent a consumer (an audit/notification row) to give the event somewhere to go. **The case for it is the showcase:** "how do you publish an event reliably when the DB commit and the message send can't share a transaction?" is a standard system-design interview question, and this is the cheapest credible answer: no broker, $0, the same Postgres. It also gives the `otel-lgtm` profile a real backlog metric to show. **It is not worth it if** shop is not meant to cover messaging at all (then this is ~7 steps × 6 backends of code that only exists to be explained), or if the user would rather show a real downstream side effect first. In that case the honest order is to add that effect (an email or ERP notification) and only then the outbox. A cheaper middle ground is to implement it in one language (Java multitier) as a worked example and skip the twins. **Known prod caveat:** prod runs on Cloud Run with `--min-instances 0` and request-based CPU (`.github/workflows/_deploy-stage.yml`), so an in-process polling relay only gets CPU while a request is in flight. Events can wait until the next request or after a scale-to-zero, which is acceptable for an audit row but worth saying out loud in the how-to (Step 5).

## ▶ Next executable step (resume here)

Open questions are resolved (G0, 2026-11-16) and the plan is confirmed **Java-only** (user, 2026-11-17). This is meta-plan unit U11 (Lane B, after U9 because of the shared place-order service and `system/db/migrations`). Start **Step 1**: confirm the `OrderPlaced` payload against the current order model (the shipping address landed in `V20261109120000__add_customer_profiles_and_order_shipping_address.sql`) and the consumer table, and write the decision into Step 1.

## Steps

- [ ] Step 1: Decide the event and its consumer. Recommendation: `OrderPlaced { orderNumber, sku, quantity, totalPrice, placedAt }`, consumed by an in-process handler that records an order-placed audit/notification row (observable in tests, no external system).
- [ ] Step 2: Flyway migration in `system/db/migrations/` (forward-only, expand-contract): an `outbox` table (`id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload jsonb`, `created_at`, `sent_at`, `attempts`), plus an index on unsent rows, and the consumer's own table with a unique key on the event id (idempotent handling).
- [ ] Step 3: Java (monolith + multitier backend + clean variant where it fits): write the outbox row in the place-order transaction; add a scheduled relay (`SELECT … FOR UPDATE SKIP LOCKED`, batch, mark sent, bounded retries with backoff); unit + component tests for the atomic write (order rolled back ⇒ no event) and the relay (crash before mark-sent ⇒ redelivered, handled once).
- [ ] Step 4: Observability hook: a span per relayed event and an `outbox_pending` gauge, so the existing opt-in `otel-lgtm` profile shows the backlog.
- [ ] Step 5: `docs/tour.md` skills-map row (Messaging / transactional outbox) and a short how-to.

## Resolved decisions

All decided (user, 2026-11-16, meta-plan G0 answer 5; scope confirmed 2026-11-17):

- **Scope: Java-only worked example** (user, 2026-11-17). Step 3 lands in the Java multitier backend (plus the Java monolith if it fits cleanly); .NET and TypeScript get no outbox code. **Shortcut, noted:** this deliberately breaks the "every change in all three languages" rule below. The shared migration still creates the tables, so the other five backends carry unused `outbox`/audit tables. Revisit if students ask for the .NET/TS versions.

- **Where the relay runs in multitier:** inside each backend process: zero infra, and it matches the monolith. No separate worker container.
- **Polling vs `LISTEN/NOTIFY`:** polling first (simplest); `LISTEN/NOTIFY` is a possible later optimisation. On Cloud Run with min instances 0 the relay only runs while the instance has CPU (see the caveat above).
- **System-test coverage:** component tests only, no new UC-001 acceptance criterion, since the event has no user-visible effect yet. Existing system tests stay unchanged.

## Constraints

- Opt-in, $0 and zero-infra by default; no broker, no new container on the default path.
- Every language change lands in Java, .NET and TypeScript, latest and legacy where applicable, **except this plan** (Java-only by decision, see Resolved decisions).
- Schema changes follow the forward-only, expand-contract doctrine in `CONTRIBUTING.md`.
