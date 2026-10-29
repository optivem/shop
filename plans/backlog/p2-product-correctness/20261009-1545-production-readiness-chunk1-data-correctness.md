# 2026-10-09 15:45:00 UTC — Production readiness, Chunk 1: data correctness

Parent plan: `20261009-1521-production-readiness-gaps.md` (Chunk 1). Chunk 0's parity audit is not yet done, so every step below starts with a check in the target language.

## TL;DR

**Why:** `placeOrder` is not atomic, coupon usage limits can be exceeded under concurrency, orders have no optimistic locking, `POST /api/orders` can't be safely retried, and list queries are unbounded.
**End result:** Placing an order is one transaction, coupon limits hold under concurrent load, concurrent order updates return 409 rather than overwriting each other, retries with an `Idempotency-Key` never duplicate, and lists are paginated. Every behaviour is covered by an acceptance scenario and holds in Java, .NET and TypeScript.

## Outcomes

- A failure after the order is saved but before the coupon count changes leaves neither change in the database.
- N concurrent orders on a coupon with limit L produce at most L successes. The rest get a clean coupon-limit validation error.
- Deliver and cancel racing on the same order: exactly one wins, and the loser gets 409 ProblemDetail.
- Replaying `POST /api/orders` with the same `Idempotency-Key` returns the original response and creates no second order.
- Order history and coupon list accept `page` and `size` with a stable sort, and respond with a bounded result.
- The order-number search no longer needs a full table scan, or the plan records why it stays as is.
- It is confirmed that `/orders/{n}` and cancel enforce the owner check for non-admin users.

## ▶ Next executable step (resume here)

Step 1.1, Java monolith (`system/monolith/java`): add an integration test in the existing `integrationTest` source set (Testcontainers Postgres) that makes `CouponService.incrementUsageCount` throw after the order is saved, then asserts that no `orders` row exists. Confirm it fails (no `@Transactional` yet), then add `@Transactional` to `OrderService.placeOrder`. Gate: `./gradlew build` and `./gradlew integrationTest`. Unblocks 1.2, which builds on the same transaction boundary.

## Findings already established (so we don't re-derive)

- `OrderService.placeOrder` calls `orderRepository.save(order)` and then `couponService.incrementUsageCount(code)`. There is no `@Transactional` anywhere.
- `CouponService.getDiscount` checks the usage limit by reading `usedCount`. `incrementUsageCount` does read-modify-write (`setUsedCount(get+1)`) with no lock. This is a check-then-act race, with a lost-update risk on the counter too.
- `createCoupon` stores a null usage limit as `Integer.MAX_VALUE`, so "unlimited" never hits the `IS NULL` branch. Any SQL change must still treat the column as nullable (the migration allows NULL).
- `Order` has no version column. Migrations: `V20260514085249__init.sql`, `V20260818113600__add_theme2_indexes.sql`, `V20261005190000__add_order_owner.sql`.
- Order numbers are `"ORD-" + UUID.randomUUID()` (122 random bits), so collision and guessability are not a concern. The only remaining question is access control, which is step 1.6.
- Request DTOs in the test-kit are string-only (see memory: request-dtos-string-only); new request fields (page, size, idempotency key) follow that rule.
- Repository queries return `List<>`: `findAllByOrderByOrderTimestampDesc`, `findByOwnerOrderByOrderTimestampDesc`, and two `LIKE '%x%'` queries on lower-cased `order_number`.

## Steps

Each numbered step follows this order: failing test first, then the fix, then the other languages. Verification gate per step: `./compile-all.sh`, plus `gh optivem system-test run --sample` for each affected language (CLAUDE.md pre-commit rules).

### 1.1 Atomic placeOrder (transaction)
- [ ] 1.1.a Java monolith: failing integration test, then `@Transactional` on `placeOrder`.
- [ ] 1.1.b Java multitier backend (`backend-java`, and check `backend-clean-java`): same.
- [ ] 1.1.c .NET (monolith and `backend-dotnet`): wrap save and coupon increment in one DB transaction (or a single `SaveChanges` unit of work). Check whether it already is one.
- [ ] 1.1.d TypeScript (monolith and `backend-typescript`): one `pg` transaction (`BEGIN`/`COMMIT`) around both writes. Check first.
- [ ] 1.1.e Acceptance scenario: add it only if it can be expressed black-box. Otherwise keep the integration test as the proof and say so in the plan.

### 1.2 Atomic coupon usage (no oversell)
- [ ] 1.2.a Replace read-modify-write with a conditional update: `UPDATE coupons SET used_count = used_count + 1 WHERE code = :code AND (usage_limit IS NULL OR used_count < usage_limit)`. If zero rows are updated, throw the existing coupon-limit `ValidationException`. Keep `getDiscount`'s limit check as a fast early rejection.
- [ ] 1.2.b Because `incrementUsageCount` now enforces the limit, order of operations in `placeOrder` matters. Decide whether to increment before saving the order (so a rejected increment never leaves an order behind). The transaction from 1.1 makes either order safe, so prefer increment-then-save.
- [ ] 1.2.c Concurrency test: coupon limit 1, N parallel `placeOrder` calls, exactly one success. Java first, then .NET and TS.
- [ ] 1.2.d Acceptance scenario for "coupon limit holds under concurrent orders" in the system-test DSL (three languages), if the DSL can issue parallel calls. If it can't, keep the concurrency test at integration level.

### 1.3 Optimistic locking on Order
- [ ] 1.3.a Flyway migration (expand only): `ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0`. Put it in `system/db/migrations` (shared by all languages) following the timestamped naming.
- [ ] 1.3.b Java: `@Version` on `Order`. .NET: a concurrency token (`RowVersion` or a `version` integer). TS: `UPDATE ... WHERE id = $1 AND version = $2`, and check the affected row count.
- [ ] 1.3.c Map the conflict to HTTP 409 ProblemDetail in `GlobalExceptionHandler` (and the .NET/TS equivalents). Add `error.types.conflict` to the config.
- [ ] 1.3.d Test: deliver and cancel racing on the same order. Exactly one succeeds and the other gets 409.
- [ ] 1.3.e Check whether the test-kit/DSL needs a way to express the 409, and whether the contract (`contracts/`) or OpenAPI docs need the new response.

### 1.4 Idempotent POST /api/orders
- [ ] 1.4.a Decide scope and TTL (open question below), then write the migration for an `idempotency_keys` table: `(owner, key)` unique, request hash, response status/body, `created_at`.
- [ ] 1.4.b Behaviour: first request stores the key and the result in the same transaction as the order. A repeat with the same key and the same request body returns the stored response. A repeat with the same key and a different body returns 422 (or 409, decide). A request without the header behaves as today, so the header stays optional.
- [ ] 1.4.c Concurrency: two simultaneous requests with the same key. The unique constraint makes the second one wait or fail, never create a duplicate. Test it.
- [ ] 1.4.d Cleanup of expired keys (scheduled delete, or document it as a later ops item).
- [ ] 1.4.e Ports to .NET and TS; add the DSL/test-kit support (header as a string, per the string-only rule) and acceptance scenarios: replay, conflicting replay, no key.

### 1.5 Pagination and search
- [ ] 1.5.a API shape: `page` (0-based or 1-based, decide) and `size` with a maximum (for example 100), a stable sort of `orderTimestamp desc, id desc`, and a response with `content`, `page`, `size`, `totalElements`. Decide whether this is a breaking change for the UI and the test-kit (see open questions).
- [ ] 1.5.b Java: `Pageable` repository methods. Add the same for the coupon list. .NET: `Skip/Take`. TS: `LIMIT/OFFSET` with a count query.
- [ ] 1.5.c UI templates and React frontend: add paging controls (or keep the default page size large enough, documented, if UI work is out of scope).
- [ ] 1.5.d Search: `LIKE '%x%'` on `LOWER(order_number)` cannot use a b-tree index. Options: (1) prefix search with an index on `LOWER(order_number)`, (2) a `pg_trgm` GIN index, (3) accept as is for a teaching dataset. Decide in the open questions, then add the migration if needed.
- [ ] 1.5.e Tests: page size cap, stable ordering across pages, and the empty page.

### 1.6 Order access control check
- [ ] 1.6.a Confirm for order details and cancel (all three languages) that a non-admin user can only read or cancel their own orders, and that a different user's order number returns 404 (not 403, so existence isn't leaked). `OrderService` line ~224 filters by owner for history; check details/cancel separately.
- [ ] 1.6.b If any path is missing the check, add a failing test first, then fix it.
- [ ] 1.6.c Record in the plan that UUID order numbers are not a security control; the owner check is.

## Out of scope for this chunk

- `@Data` on JPA entities (parent plan step 1.7). It is Java-only, low risk and can ride along with 1.3 when `Order` is edited anyway. Decide then.
- Resilience, observability, security headers and so on (parent chunks 2–6).

## Open questions

- **Idempotency scope and TTL (1.4):** per authenticated user, 24 hours. Recommended, because it matches the common payment-API practice. Needs your OK.
- **Same key, different body (1.4):** 422 or 409? Recommended 422 (the same choice Stripe made), but any is fine as long as it is documented.
- **Pagination as a breaking change (1.5):** the UI and test-kit currently expect full lists. Recommended: add `page` and `size` as optional with a default size of 50 and a hard cap of 100, so existing callers keep working. Confirm that UI paging controls are in scope.
- **Search indexing (1.5.d):** recommended option (3) for a teaching dataset (document the trade-off, no migration), or option (2) `pg_trgm` if you want to demonstrate the fix. Your call.
- **Which acceptance scenarios can be black-box (1.1.e, 1.2.d):** parallel calls may not be expressible in the current DSL. Recommended: integration tests for races, acceptance scenarios for the observable single-call behaviours (replay, 409 mapping, pagination).
- **Legacy twins:** leave frozen, as in the parent plan.
- **Order of steps:** recommended 1.1, 1.2, 1.3, 1.6, 1.4, 1.5. The first three are small, related and need no API change; 1.6 is a quick audit; 1.4 and 1.5 change the API surface.
