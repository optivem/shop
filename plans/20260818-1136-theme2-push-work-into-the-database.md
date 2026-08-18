# 2026-08-18 11:36 UTC — theme 2: the database is barred from the work it does best (`backend-clean-java`)

**Scope: `system/multitier/backend-clean-java` only.** Nothing in this plan touches another project,
another plan, or the legacy backends.

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
| Enforcing invariants transactionally | **No `@Transactional` anywhere in `src/main`** | verified by grep, zero hits |

## TL;DR

**Why:** The clean variant has no set-based write anywhere, no aggregate query, no limit on any read,
and no transaction boundary. Coupon redemption is a read-modify-write lost update: two concurrent
orders on the same coupon both read `used=4` and both write `used=5`, so the usage limit does not
hold. The demo has nothing to show for the theme the course frames as the hardest — and one place
that demonstrates the anti-pattern.

**End result:** Four demonstrations in `backend-clean-java`, each one a capability the database has
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
- **Clean variant only.** The legacy backends and monoliths are the frozen before-picture. The
  "before" is shown live as a git diff during the talk, not built twice.
- **Ports keep their abstraction.** No `Pageable`, no `EntityManager`, no SQL string in any port.
  Every item changes what a port *says*, never whether it exists.

## ▶ Next executable step (resume here)

**Chunk 0 — the measurement harness.** It comes first on purpose: the claim is *"the system pays for
it in performance"*, and a demo that cannot show the before-numbers is asserting, not demonstrating.
Concretely: write `system/db/seed/demo-volume.sql` (a `generate_series` insert, see item 0.1), then
capture baseline timings and `EXPLAIN (ANALYZE, BUFFERS)` for today's in-memory behaviour, before a
single production line changes.

Then Chunks A → B → C, in that order, one per `/clear`-ed session. They are independent; the order
is by how directly each answers the thesis.

## Chunk 0 — measure first

- [ ] **0.1 Seed script.** `system/db/seed/demo-volume.sql` — **not** a Flyway migration (it must
      never run in CI or production). One `INSERT … SELECT … FROM generate_series(1, 100000)`
      producing 100k orders across a spread of SKUs, countries, statuses and timestamps, plus a few
      hundred coupons with orders referencing them. Postgres generates this in seconds.
- [ ] **0.2 Baseline numbers.** With the seed loaded, record for each of the four capabilities: wall
      time, row count returned, and `EXPLAIN (ANALYZE, BUFFERS)`. These are the talk's "before"
      slide. Capture them into `docs/atdd/code/theme2-measurements.md` so the numbers survive the
      session.
- [ ] **0.3 A repeatable way to re-run it.** A Gradle task or a shell script — whatever is cheapest —
      so the after-numbers are measured the same way as the before-numbers rather than by hand.

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
      statements; if the second fails the order exists un-redeemed. See Q1 — a `TransactionRunner`
      port, because `USECASES_ARE_FRAMEWORK_FREE_EXCEPT_JAKARTA_VALIDATION` forbids annotating the
      use case, and a transaction is precisely an infrastructure mechanism the use case must control.
- [ ] **A6. Endpoints + index.** Per Q2. Index `idx_orders_sku_status ON orders (sku, status)` folded
      into the single migration (item C4).

## Chunk B — aggregation and joins (a read model with no domain entity)

- [ ] **B1. The read-side port.** Per Q3: `usecases/queries/SalesReportQuery`, implemented by
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
- [ ] **C2. Rename the leaked port methods.** `domain/repositories/OrderRepository:18,20` declares
      `findAllByOrderByOrderTimestampDesc()` and
      `findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(String)` — Spring Data's
      query-derivation DSL spelled into a port whose own javadoc claims *"no Spring Data, no JPA"*.
      **This is the thesis in miniature: the port names a mechanism, so the caller can only loop.**
      Rename to `findRecentFirst(PageRequest)` and `searchByOrderNumber(String, PageRequest)`; the
      Spring Data naming stays inside `infrastructure/persistence/repositories/`. Same for
      `CouponRepository.findAll()` → `findAll(PageRequest)`.
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
- [ ] **C5. Thread paging through use cases, DTOs, controllers, frontend.** Requests gain size +
      cursor; responses gain `nextCursor` + `hasMore`. The wire cursor is an opaque base64 token
      encoded in `presentation`; the domain carries the typed `OrderCursor`. Frontend per Q4.

## Open questions

- [ ] **Q1. Transaction boundary shape.** **Recommendation: a `usecases/TransactionRunner` port**
      (`<T> T inTransaction(Supplier<T>)`) over Spring's `TransactionTemplate`, injected by
      `UseCaseConfig`. The alternatives — annotating the use case (banned by ArchUnit) or the
      controller (wrong layer) — are both worse.
- [ ] **Q2. Do the bulk operations get HTTP endpoints, or are they ops/scheduled tasks?**
      **Recommendation: endpoints.** `POST /api/admin/recall/{sku}` and
      `POST /api/admin/orders/sweep-deliveries`. Demoable live, testable by the system-test suite, and
      a scheduler can be added later without changing the use case.
- [ ] **Q3. Where does the read-side port live?** **Recommendation: `usecases/queries/`.** The report
      is deliberately not a domain repository — putting it in `domain/repositories/` would assert it
      *is* domain, which is the claim the demo refutes. `USECASES_DEPEND_ONLY_INWARD` and
      `SPRING_DATA_IS_CONFINED_TO_INFRASTRUCTURE` both stay satisfied.
- [ ] **Q4. Pagination default page size, and does the React table get "Load more"?**
      **Recommendation: default 50, `size` + `cursor` params, "Load more" on the tables.**
      Partially verified 2026-08-18, with one caveat to close first:
      - Per test the counts are small — the busiest spec (`place-order-positive-test.spec.ts`) has 28
        `placeOrder()` mentions across *all* its tests combined.
      - **But no database truncation is visible between tests** — no `TRUNCATE`/reset helper anywhere
        under `system-test/{typescript/src,dotnet,java}` outside build artifacts — so rows accumulate
        across a full run and 50 could be exceeded by the end of one.
      - Whether that breaks anything is still unknown: there is no `browseOrderHistory` call in
        `system-test/typescript/tests/latest/acceptance` at all, so order history may only be
        exercised through the UI channel.
      **Close it by checking how the order-history and coupon-list assertions select rows.** If they
      assert on a specific order number, cumulative growth is harmless. If any asserts "the list has
      exactly N", pagination breaks it regardless of the default.
- [ ] **Q5. Does the sales report get a UI page?** **Recommendation: API-only**, covered by a system
      test on the API channel. A reports page adds front-end work and nothing architectural.

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
