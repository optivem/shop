# 2026-08-18 11:36 UTC — theme 2: the database is barred from the work it does best (`backend-clean-java`)

**Scope: `system/multitier/backend-clean-java` only.** No other backend, no frontend, no legacy
project, no other plan. A short list of files falls outside the backend directory by necessity and
nothing else does: the demo seed script and its README under `system/db/seed/` and the additive index
migration (C4) under `system/db/migrations/`, both of which must live in the shared `system/db/` tree
the Flyway sidecar owns; `docs/atdd/code/theme2-measurements.md`, where the before/after numbers are
recorded; and `docs/atdd/code/language-equivalents.md`, where the cross-language gate records
decisions for the clean .NET and TypeScript backends that do not exist yet. None of them changes
behaviour in another project — `system/db/seed/` in particular is outside every `db-migrate` mount,
which names `system/db/migrations` explicitly.

## The thesis

> **Business logic forced into memory, so the system gets slow.** The rule *"the database is only for
> I/O"* is applied everywhere, so bulk updates get pulled into the application layer. So the database
> is barred from the very work it does best, and the system pays for it in performance.

The dogma is not stupid — *"the database is a detail"* is good advice against vendor lock-in and
ORM-shaped domains, and `backend-clean-java` follows it well (theme 1 is already the strongest part
of this codebase). The failure is applying it to **capability** instead of to **coupling**.
Decoupling from the database's dialect is not the same as refusing its set operations.

**Where it actually goes wrong is the port's vocabulary.** A port that names a *mechanism* —
`findAll()` — forces the loop into the application layer, because "give me all the rows" is the only
question you can ask it. A port that names an *intent* — `recallSku(sku)` — leaves the adapter free
to answer with one statement. The abstraction survives; only the loop dies.

That is the spine of every item below: **keep the port, change what it says.**

## What the database does best, and where this codebase forbids it

| The database is good at | `backend-clean-java` does it | Where |
|---|---|---|
| Set-based writes (`UPDATE … WHERE`) | Nothing bulk exists; any multi-row write would be N round trips | no call site yet — Chunk A creates the honest one |
| Atomic read-modify-write | In memory, and **it is a lost update** | `PlaceOrder:96-99` + `UsageQuota.recordUse()` |
| Aggregation and joins | No report exists; written naively it is `findAll()` + Java streams | Chunk B creates it |
| Filtering, sorting, limiting | Partly pushed down; **unbounded** and fully hydrated | `BrowseCoupons`, `BrowseOrderHistory` |
| Projecting only the columns asked for | **Never** — every read builds the full domain model, then unwraps it | `BrowseCoupons`, `BrowseOrderHistory`, `ViewOrderDetails` — Chunk R |
| Enforcing invariants transactionally | **No `@Transactional` anywhere in `src/main`** | verified by grep, zero hits |

## TL;DR

**Why:** The clean variant has no set-based write anywhere, no aggregate query, no limit on any read,
and no transaction boundary. Coupon redemption is a read-modify-write lost update: two concurrent
orders on the same coupon both read `used=4` and both write `used=5`, so the usage limit does not
hold. The demo has nothing to show for the theme the course frames as the hardest — and one place
that demonstrates the anti-pattern.

**End result:** Five demonstrations in `backend-clean-java`, each one a capability the database has
and the application layer was doing instead — with measured before/after numbers, not assertions.
The schema keeps its exact current shape; only indexes are added.

## Settled — do not reopen

- **Schema shape does not change.** Verified column-by-column against
  `system/db/migrations/V20260514085249__init.sql`: every item below runs on columns that already
  exist. No new tables, no new columns, no type changes. JPA entities and mappers are therefore
  untouched, and `ddl-auto: validate` keeps passing (Hibernate validates tables/columns/types, not
  indexes).
- **`V20260514085249__init.sql` is never edited.** Flyway checksums applied migrations; editing it
  breaks every existing database. Indexes land in one new additive file.
- **No frontend change, ever.** `system/multitier/frontend-react` is a single shared project — every
  `gh-optivem-multitier-*.yaml` (java, dotnet, typescript × latest, legacy) points at the same path,
  and none of them points at `backend-clean-java`. Touching it would change a component owned by six
  other SUTs to consume a wire shape only this backend returns. Paging is demonstrated over the API.
- **Clean variant only.** The legacy backends and monoliths are the frozen before-picture. The
  "before" is shown live as a git diff during the talk, not built twice.
- **Ports keep their abstraction.** No `Pageable`, no `EntityManager`, no SQL string in any port.
  Every item changes what a port *says*, never whether it exists.

## Decisions — all open questions resolved 2026-08-18

- **Transaction boundary → a `usecases/TransactionRunner` port** (`<T> T inTransaction(Supplier<T>)`)
  over Spring's `TransactionTemplate`, injected by `UseCaseConfig`. Annotating the use case is banned
  by `USECASES_ARE_FRAMEWORK_FREE_EXCEPT_JAKARTA_VALIDATION`; annotating the controller is the wrong
  layer. A transaction is an infrastructure mechanism the use case must nonetheless control, which is
  exactly what a port is for — and is itself course material.

- **Read-side port → `usecases/queries/SalesReportQuery`**, implemented by
  `infrastructure/persistence/queries/JpaSalesReportQuery`. Not `domain/repositories/`: putting it
  there would assert the report *is* domain, which is the claim the demo refutes.
  `USECASES_DEPEND_ONLY_INWARD` and `SPRING_DATA_IS_CONFINED_TO_INFRASTRUCTURE` both stay satisfied.

  **Extended 2026-08-18 to the reads that already exist — this is Chunk R.** The same placement rule
  governs `OrderQuery` / `CouponQuery`. Note that the placement is not what bypasses the domain model;
  the projection return type is. Placement is what makes the code *admit* it: a port in
  `domain/repositories/` claims the domain needs it, and the domain never calls this one. Checked: no
  existing ArchUnit rule pins a port to the domain package.

- **HTTP surface → bulk endpoints and a report endpoint; no UI of any kind.**
  `POST /api/admin/recall/{sku}`, `POST /api/admin/orders/sweep-deliveries`, `GET /api/reports/*`.
  All demoable live and coverable by API-channel system tests. Nothing here adds front-end work: the
  demonstrations are architectural and the API is the whole surface.

- **List ordering under pagination → orders newest-first; coupons `ORDER BY id DESC` in the adapter.**
  Default page size 50, `size` + `cursor` query params. Paging stops at the API boundary — no consumer
  of it is built.

### Why the ordering decision is what makes the suite safe

The original worry — "is a default of 50 big enough?" — was the wrong question. `BrowseCouponsVerification.FindCouponByCode`
(`system-test/dotnet/Dsl.Core/UseCase/UseCases/BrowseCouponsVerification.cs:58-70`) does
`Response.Coupons.FirstOrDefault(c => c.Code == couponCode)` — it searches **only the returned
list**, which under pagination is page 1. So the real question is *"is the coupon under test on page
one?"*, and the answer must hold no matter how many rows have accumulated.

- **Orders**: sort `order_timestamp DESC, order_number DESC`. Each test's own order is the newest, so
  it is always on page 1. Safe by construction, and it is the ordering the endpoint should have
  anyway.
- **Coupons**: the table has **no timestamp column** (`id, code, discount_rate, valid_from, valid_to,
  usage_limit, used_count`), so "newest" has to come from the surrogate `id`. `ORDER BY id DESC`
  **inside the adapter** gives newest-published-first; the `id` never leaves `infrastructure`, so the
  domain rule (no `Long id` in domain entities) still holds. The keyset cursor is `code`, which is
  `UNIQUE` and domain-visible, so no tiebreaker is needed.
  Rejected: `ORDER BY code` — architecturally cleaner as a keyset key, but it puts each test's coupon
  at an arbitrary page and forces the .NET DSL and the UI page-object to page until found. Rejected:
  adding `created_at` — it would break this plan's no-schema-change promise.

Also found while checking: the TypeScript `ThenBrowseCoupons` assertions are effectively no-ops
(`errorMessage`/`fieldErrorMessage` just `return this`), so only the .NET DSL and the UI channel
actually assert on list contents. And no database truncation is visible between tests anywhere under
`system-test/{typescript/src,dotnet,java}` — rows accumulate across a run. Both facts are moot given
newest-first ordering, but they matter if the ordering is ever changed.

## ▶ Next executable step (resume here)

**Chunk A — set-based writes (the headline).** Chunk 0 is done: `system/db/seed/demo-volume.sql`
exists, `./gradlew benchmark` in `backend-clean-java` re-takes the numbers the same way every time,
and the before-slide is written up in `docs/atdd/code/theme2-measurements.md`. So there is now a
baseline to beat, and everything below can be measured rather than asserted.

Concretely, and in this order: write **A4** first (the concurrency test that fails today) so the lost
update is real before A3 fixes it; then **A1** (`RecallSku` + `cancelOutstandingForSku` on
`OrderRepository`), **A2** (the delivery sweep), **A3** (`tryRedeem`), **A5** (the `TransactionRunner`
port), **A6** (the two admin endpoints; its index folds into C4's single migration, which is *not*
written in this chunk). Then re-run `./gradlew benchmark` and append the after-table to
`docs/atdd/code/theme2-measurements.md` — the harness already prints the recall's before-numbers
(9,093 ms, 6,001 JDBC statements to change 2,000 rows) for A1 to be compared against.

Then Chunks R → B → C, one per `/clear`-ed session. All independent except item C2, which depends on
Chunk R; the order is by how directly each answers the thesis.

## Chunk A — set-based writes (the headline)

- [ ] **A1. Bulk SKU recall.** The ERP reports a product withdrawn, so every outstanding order for
      that SKU is cancelled. New `usecases/order/RecallSku`; port method
      `int cancelOutstandingForSku(String sku)` on `OrderRepository` — **an intent, not a mechanism**.
      Adapter implements it as one `UPDATE orders SET status = 'CANCELLED' WHERE sku = :sku AND
      status <> 'CANCELLED'` returning rows-affected, against the in-memory alternative of 1 + N
      selects and N updates.
      **Write the trade-off into the class javadoc.** `Order.cancel()` (`Order.java:58`) owns the rule
      *"cancellable from any status but cancelled"*, and the set-based write restates that invariant
      as its `WHERE` clause instead of routing through the entity. What is lost (per-entity domain
      events, the rule stated once) and what is bought (atomicity, one round trip) is the discussion —
      it belongs where the next reader trips over it, not only in the talk.
- [ ] **A2. Nightly delivery sweep.** The second flavour of set-based write, near-free once A1 lands:
      mark every `PLACED` order older than N days `DELIVERED`. `Order.deliver()` (`Order.java:50`)
      already says *"only from PLACED"*, which maps to `WHERE status = 'PLACED'` exactly. This is the
      batch case where N round trips genuinely hurt, and the mapping from entity rule to `WHERE`
      clause is cleaner than A1's — worth showing both.
- [ ] **A3. Atomic coupon redemption.** The sharpest item, because the in-memory version is not merely
      slow, it is **wrong**. Replace the read-modify-write at `PlaceOrder:96-99` with a conditional
      update: `boolean tryRedeem(CouponCode)` on the port, implemented as
      `UPDATE coupons SET used_count = used_count + 1 WHERE code = :code AND (usage_limit IS NULL OR
      used_count < usage_limit)` with a rows-affected check. `false` means the coupon was exhausted
      concurrently and becomes a `UseCaseError`.
      **Surface the tension rather than resolving it away:** `UsageQuota.exhausted()` stays, because
      `Coupon.discountAt` uses it to fail fast with a good message on the read path. The rule now
      lives in two places *on purpose* — the database is authoritative, memory is the fast path.
      Saying which is which, out loud, is the answer to "application layer or database?"
- [ ] **A4. A concurrency test that fails today.** Two threads redeeming the last unit of one coupon;
      assert exactly one succeeds. Write it before A3 and watch it fail — that is what makes the lost
      update real rather than theoretical.
- [ ] **A5. Transaction boundary.** `PlaceOrder` writes the order and the redemption as two unrelated
      statements; if the second fails the order exists un-redeemed. Use the decided `TransactionRunner`
      port, because `USECASES_ARE_FRAMEWORK_FREE_EXCEPT_JAKARTA_VALIDATION` forbids annotating the
      use case, and a transaction is precisely an infrastructure mechanism the use case must control.
- [ ] **A6. Endpoints + index.** `POST /api/admin/recall/{sku}` and
      `POST /api/admin/orders/sweep-deliveries`, per the decided HTTP surface. Index `idx_orders_sku_status ON orders (sku, status)` folded
      into the single migration (item C4).

## Chunk R — light CQRS: the read path stops going through the domain

**The census (2026-08-18, verified against `UseCaseConfig:30-63`).** All seven use cases split
cleanly, with no borderline case:

| | Use case | Verdict |
|---|---|---|
| **Commands** | `PlaceOrder`, `CancelOrder`, `DeliverOrder`, `PublishCoupon` | stay on the domain path — each asks an entity to decide something |
| **Queries** | `BrowseCoupons`, `BrowseOrderHistory`, `ViewOrderDetails` | every response field is a stored column; the domain model is built and immediately unwrapped |

The test that produced that split, and the one to apply to any future use case: **does the response
hold a field the database does not already hold?** If yes it is not a pure query and the domain
stays in the loop. If the coupon list ever gained a `redeemableNow` flag, that is `Coupon.discountAt`
semantics (validity ∧ quota) and `BrowseCoupons` leaves this chunk — the same tension A3 flags for
`UsageQuota.exhausted()`.

**Demo `ViewOrderDetails` first — it is the sharpest of the three.** Fifteen response fields
(`ViewOrderDetailsResponse:15-29`), every one a column in `orders`, reached by constructing seven
`Money`, two `Rate`, a `Country` and a `CouponCode` and then calling `.amount()` / `.value()` on each
(`ViewOrderDetails:36-50`). Nothing built on that path survives to the wire.

**The second argument is stronger than performance.** `Coupon`'s constructor rejects a
`discountRate` of zero (`Coupon.java:44-47`), so one bad row makes the whole *list* endpoint 500. A
read path must not be able to fail on a write-side invariant — that is the CQRS case stated as a bug
rather than a preference, and it is what makes this chunk more than a micro-optimisation.

**What is given up, and it goes in the javadoc rather than being resolved away:** the read model can
now drift from the domain's idea of a coupon, because nothing forces the two to agree. Speed and
failure-isolation are bought by surrendering the guarantee that what is displayed was validated by
the rules that wrote it.

- [ ] **R1. The read-side ports.** `usecases/queries/OrderQuery` and `usecases/queries/CouponQuery`,
      implemented by `infrastructure/persistence/queries/JpaOrderQuery` / `JpaCouponQuery`. Flat
      records, `BigDecimal` money fields, **no value objects and no `Guard`** — same rule as B1, and
      the javadoc says why. `usecases/queries/` and not `domain/repositories/`: a port in the domain
      claims the domain needs it, and the domain never calls this one — see the read-side port
      decision above.
- [ ] **R2. `BrowseCoupons`.** Projection record `CouponListItem` over the six `coupons` columns; the
      use case copies primitives into `BrowseCouponsResponse`. A dedicated record rather than
      projecting SQL straight into the response item, because `ArchitectureTest:117` pins
      `Request`/`Response` to `usecases.order` / `usecases.coupon`, and keeping the wire contract out
      of the JPQL means a field rename does not edit a query string.
- [ ] **R3. `BrowseOrderHistory`.** One port method taking the optional order-number filter, so the
      `if` at `BrowseOrderHistory:29-33` collapses to a single call and the `LIKE` stays in SQL.
- [ ] **R4. `ViewOrderDetails`.** Returns `Optional<OrderDetail>`; empty still becomes
      `UseCaseError.NotFound` (`ViewOrderDetails:29-31`), so the error contract does not move.
- [ ] **R5. Delete the orphaned port methods — but not all of them.** `CouponRepository.findAll()`
      and `OrderRepository:18,20` have exactly one caller each, all three in R2/R3, so they are
      **deleted, not renamed**. **`OrderRepository.findByOrderNumber` stays**, because `CancelOrder:37`
      and `DeliverOrder:29` need the real entity to call `cancel()` / `deliver()`; `ViewOrderDetails`
      gets a projection twin on the query port instead. The same question gets two answers — an entity
      for the command, a projection for the query — and saying that out loud is the chunk's best aside.
      End state: `CouponRepository{save, findByCode}`, `OrderRepository{save, findByOrderNumber}`.
      `CouponRepositoryIntegrationTest:90` uses `findAll()` incidentally; switch it to `findByCode`.
- [ ] **R6. An ArchUnit rule that states the claim.** `READ_USECASES_DO_NOT_TOUCH_THE_DOMAIN` — no
      class in `usecases.queries..`, and neither of the three query use cases, may depend on
      `..domain..`. Today `BrowseCoupons:3-4` imports `Coupon` and `CouponRepository`; after R2 it
      imports neither, and the rule is the light-CQRS claim made executable. Nothing blocks it: none
      of the ten existing rules pins a port to the domain package.
- [ ] **R7. Numbers.** Reuse the Chunk 0 harness. Here the measurement is objects-not-constructed as
      much as wall time: over the 100k seed, `BrowseOrderHistory` builds 100k `Order` aggregates plus
      their value objects today and none after R3.

## Chunk B — aggregation and joins (a read model with no domain entity)

- [ ] **B1. The read-side port.** `usecases/queries/SalesReportQuery`, implemented by
      `infrastructure/persistence/queries/JpaSalesReportQuery`. Flat records —
      `RevenueByCountryMonth`, `TopSkuByRevenue`, `CouponEffectiveness` — `BigDecimal` money fields,
      **no value objects and no `Guard`**. State that absence in the javadoc: these are projections,
      and running 100k rows through `Money`/`Country`/`Guard` to produce numbers that go straight to
      JSON is the cost the demo exists to avoid.
- [ ] **B2. Revenue by country and month.**
      `SELECT country, date_trunc('month', order_timestamp) AS month, COUNT(*), SUM(quantity),
      SUM(subtotal_price), SUM(tax_amount), SUM(total_price) FROM orders WHERE status <> 'CANCELLED'
      GROUP BY country, month ORDER BY month DESC` — native (`date_trunc` is Postgres-specific), which
      is fine: it lives in `infrastructure`, and dialect coupling *there* is what the layer is for.
- [ ] **B3. Top SKUs by revenue.** `GROUP BY sku ORDER BY SUM(total_price) DESC LIMIT :n`. Takes a
      plain limit — deliberately **not** coupled to Chunk C's page vocabulary, so the chunks stay
      independent.
- [ ] **B4. Coupon effectiveness.** The strongest single case, because in memory it is two
      `findAll()`s and a hand-rolled join:
      `SELECT c.code, c.usage_limit, c.used_count, COUNT(o.id), COALESCE(SUM(o.discount_amount), 0)
      FROM coupons c LEFT JOIN orders o ON o.applied_coupon_code = c.code AND o.status <> 'CANCELLED'
      GROUP BY c.code, c.usage_limit, c.used_count`.
- [ ] **B5. Use case + endpoint.** `usecases/report/ViewSalesReport` implementing
      `UseCase<ViewSalesReportRequest, ViewSalesReportResponse>` — the
      `USECASES_IMPLEMENT_THE_USECASE_INTERFACE` ArchUnit rule requires it — and
      `presentation/controller/ReportController` matching the existing controllers' OpenAPI style.

## Chunk C — volume (stop loading the table)

- [ ] **C1. Domain-owned page vocabulary.** `domain/queries/PageRequest`, `Page<T>`,
      `OrderCursor(Instant orderTimestamp, String orderNumber)`. Spring's `Pageable`/`Page` must not
      appear in any port.
      **The cursor's tiebreaker is `order_number`, not the surrogate `id`** — clean-java deliberately
      keeps `Long id` out of the domain (`OrderRepositoryAdapter#save` resolves it). Textbook keyset
      pagination reaches for `id`; this constraint forces the honest version, and it is a good aside.
- [ ] **C2. Paging lands on the read side, not on the repositories.** **Depends on Chunk R** — the
      plan's only cross-chunk dependency. R5 deletes the three mechanism-named read methods
      (`CouponRepository.findAll()`, `OrderRepository:18,20`) from the domain repositories outright,
      so there is nothing left here to rename: `PageRequest` is threaded onto the `usecases/queries/`
      ports instead, which after Chunk R is where every unbounded read lives.
      **The original point survives and gets sharper.** Those names —
      `findAllByOrderByOrderTimestampDesc` and
      `findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc` — are Spring Data's
      query-derivation DSL spelled into a port whose own javadoc claims *"no Spring Data, no JPA"*,
      and because the port named a mechanism the caller could only loop. Show them as the
      before-picture in the live git diff; the after is that they do not exist at all.
- [ ] **C3. Keyset, not `OFFSET`.** Row-value comparison —
      `(o.orderTimestamp, o.orderNumber) < (:ts, :num) ORDER BY … DESC LIMIT :size`. `OFFSET 10000`
      makes Postgres walk 10,000 rows to discard them; with 100k seeded rows the `EXPLAIN` contrast is
      the demo.
- [ ] **C4. The one migration.** `system/db/migrations/V20260818113600__add_theme2_indexes.sql`,
      additive, all four indexes together:
      `idx_orders_recent (order_timestamp DESC, order_number DESC)`,
      `idx_orders_sku_status (sku, status)`,
      `idx_orders_status_country_ts (status, country, order_timestamp)`,
      and the partial `idx_orders_applied_coupon (applied_coupon_code) WHERE applied_coupon_code IS NOT NULL`
      — most orders carry no coupon, so indexing only the rows that do is both smaller and faster, and
      is a good aside in its own right.
- [ ] **C5. Thread paging through use cases, DTOs, controllers.** Requests gain size + cursor;
      responses gain `nextCursor` + `hasMore`. The wire cursor is an opaque base64 token encoded in
      `presentation`; the domain carries the typed `OrderCursor`. Default page size 50. The chain
      stops at the controller — no frontend change.

## Cross-language gate

`backend-clean-dotnet` / `backend-clean-typescript` do not exist yet, so each decision goes into
`docs/atdd/code/language-equivalents.md` before they do — port one model into two languages rather
than change three codebases later.

- **Intent-named ports over mechanism-named ones.** The rule is language-independent and is the
  headline: `recallSku(sku)`, not `findAll()` + loop.
- **Set-based write.** .NET `ExecuteUpdateAsync` with a predicate; TypeScript Prisma `updateMany` with
  a where-guard. Both return a count that must be checked.
- **Read-model port separate from the repository.** .NET `ISalesReportQuery`, TS `SalesReportQuery` —
  owned by the use case layer, not the domain.
- **Pure queries bypass the domain model entirely.** Not just the new report — any use case whose
  response holds nothing the database does not already hold (Chunk R's census test). .NET projects
  with `Select` into a record plus `AsNoTracking`; TypeScript with a Prisma `select` clause. Neither
  materialises an entity, and neither read path can fail on a write-side invariant.
- **Transaction boundary as a port.** .NET over `IDbContextTransaction`; TS over Prisma `$transaction`.
- **Domain-owned paging.** Never `IQueryable`/EF `Skip`/`Take` or Prisma `take`/`cursor` in a port.

## Non-goals

- **Every other plan in `plans/`.** This plan coordinates nothing but itself. The only other plan
  touching `backend-clean-java` is `20260817-1448`, whose single remaining item is a deferred
  `@ExceptionHandler` in `GlobalExceptionHandler` — a file nothing here touches. No conflict, no
  parent plan needed.
- **The legacy backends and monoliths.** Frozen before-picture.
- **A materialized view or summary table.** Would need the schema change this plan deliberately avoids;
  the indexes in C4 are enough at demo scale.
- **A Pact contract for the new endpoints.** The component/contract layer is opt-in; separate work.
