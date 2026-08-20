# 2026-08-18 11:36 UTC — theme 2: the database is barred from the work it does best (`backend-clean-java`)

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-08-20T11:33:30Z`

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

The rows Chunks A and R answered are struck through; the rest are still the before-picture.

| The database is good at | `backend-clean-java` does it | Where |
|---|---|---|
| ~~Set-based writes (`UPDATE … WHERE`)~~ | **Done (A1, A2).** `RecallSku` and `SweepDeliveries` over `OrderRepository.cancelOutstandingForSku` / `deliverPlacedOlderThan` | `usecases/order/`, `POST /api/admin/*` |
| ~~Atomic read-modify-write~~ | **Done (A3, A4).** One conditional `UPDATE`; the lost update is pinned by `CouponRedemptionConcurrencyIntegrationTest` | `CouponRepository.tryRedeem` |
| ~~Aggregation and joins~~ | **Done (B1–B5).** `SalesReportQuery` answers three questions with three `GROUP BY`s; `GET /api/reports/sales` | `usecases/queries/`, `usecases/report/`, `infrastructure/persistence/queries/` |
| ~~Filtering, sorting, limiting~~ | **Done (C1–C5).** Keyset pages over `usecases/queries/PageSpec` + `Page<T>`; `size` + opaque `cursor` on `GET /api/orders` and `GET /api/coupons`; four indexes in one additive migration | `usecases/queries/`, `infrastructure/persistence/queries/`, `presentation/` |
| ~~Projecting only the columns asked for~~ | **Done (R1–R6).** The three query use cases read flat projections off `usecases/queries/`; `READ_USECASES_DO_NOT_TOUCH_THE_DOMAIN` keeps them there | `usecases/queries/`, `infrastructure/persistence/queries/` |
| ~~Enforcing invariants transactionally~~ | **Done (A5).** The order and its redemption are one unit, declared by a port rather than an annotation | `usecases/TransactionRunner` |

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

**R7 — take the after-numbers. It is the only item left in this plan.** Chunks 0, A, R1–R6, B and C
are done. Chunk C landed the read side's page vocabulary in `usecases/queries/` (`PageSpec<C>`,
`Page<T>`, `OrderCursor`) rather than in `domain/queries/` as C1 originally said —
`READ_USECASES_DO_NOT_TOUCH_THE_DOMAIN`, added by Chunk R after C1 was drafted, forbids a query port
from naming a domain type, and paging is a read-side concern in any case. `JpaOrderQuery` pages on
the row-value comparison `(order_timestamp, order_number) < (:ts, :num)` and `JpaCouponQuery` on
`id < (SELECT id FROM coupons WHERE code = :cursorCode)`, both native and both `LIMIT size + 1` so
`hasMore` costs no second query. `presentation/CursorCodec` is the only place that knows the token is
base64.

R7 needs Docker and takes minutes: start the stack, run `./gradlew benchmark` in
`system/multitier/backend-clean-java`, and append the table it writes to
`build/benchmark/theme2-baseline.md` into `docs/atdd/code/theme2-measurements.md` beside the Chunk 0
baseline.

**Run `./gradlew integrationTest` in the same Docker session, before or after the benchmark.** Chunk
C's keyset SQL is native and was written without a database to run it against;
`KeysetPagingIntegrationTest` is the test that proves it walks every row exactly once, and it has
never been executed.

## Chunk R — light CQRS: the read path stops going through the domain

R1–R6 are done; only the measurement is left. The reasoning below is kept because it is the demo's
script and the test any future use case has to pass.

**The census.** All seven use cases split cleanly, with no borderline case:

| | Use case | Verdict |
|---|---|---|
| **Commands** | `PlaceOrder`, `CancelOrder`, `DeliverOrder`, `PublishCoupon` | stay on the domain path — each asks an entity to decide something |
| **Queries** | `BrowseCoupons`, `BrowseOrderHistory`, `ViewOrderDetails` | every response field is a stored column; the domain model is built and immediately unwrapped |

The test that produced that split, and the one to apply to any future use case: **does the response
hold a field the database does not already hold?** If yes it is not a pure query and the domain
stays in the loop. If the coupon list ever gained a `redeemableNow` flag, that is `Coupon.discountAt`
semantics (validity ∧ quota) and `BrowseCoupons` leaves this chunk — the same tension A3 flags for
`UsageQuota.exhausted()`.

**Demo `ViewOrderDetails` first — it is the sharpest of the three.** Fifteen response fields, every
one a column in `orders`, reached on the old path by constructing seven `Money`, two `Rate`, a
`Country` and a `CouponCode` and then calling `.amount()` / `.value()` on each. Nothing built on that
path survived to the wire; the git diff of `ViewOrderDetails` is the whole argument in one screen.

**The second argument is stronger than performance.** `Coupon`'s constructor rejects a
`discountRate` of zero (`Coupon.java:44-47`), so one bad row makes the whole *list* endpoint 500. A
read path must not be able to fail on a write-side invariant — that is the CQRS case stated as a bug
rather than a preference, and it is what makes this chunk more than a micro-optimisation.

**What is given up, and it goes in the javadoc rather than being resolved away:** the read model can
now drift from the domain's idea of a coupon, because nothing forces the two to agree. Speed and
failure-isolation are bought by surrendering the guarantee that what is displayed was validated by
the rules that wrote it.

- [ ] **R7. Numbers.** The harness is already updated: `Theme2Baseline` reads the before-picture
      straight off the JPA repositories (the domain ports no longer offer `findAll`), and the three
      browse/view rows now report zero domain objects. What is left is to run `./gradlew benchmark`
      against the 100k seed and append the after-numbers to `docs/atdd/code/theme2-measurements.md`
      beside the Chunk 0 baseline. The headline is objects-not-constructed as much as wall time:
      `BrowseOrderHistory` built 100k `Order` aggregates plus their value objects before R3 and
      builds none after it.
      **Chunk C changed what these three probes measure and the write-up must say so**: they now
      report the first page at the default size of 50, because after C2 the unbounded read is not a
      request the port can express. Comparing "100k rows" against "50 rows" is the demonstration, not
      a discrepancy — the harness labels the rows "first page" for exactly that reason.

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
- **Read-side-owned paging.** The page vocabulary belongs beside the query ports, not in the domain:
  nothing on the command side pages, and in Java `READ_USECASES_DO_NOT_TOUCH_THE_DOMAIN` makes a
  domain placement fail the build outright. Never `IQueryable`/EF `Skip`/`Take` or Prisma
  `take`/`cursor` in a port, and never a framework's own `PageRequest`. Keyset, never `OFFSET`; the
  cursor reaches the wire base64-encoded and is decoded in the presentation layer, so no client ever
  learns the sort key.

## Non-goals

- **Every other plan in `plans/`.** This plan coordinates nothing but itself. The only other plan
  touching `backend-clean-java` is `20260817-1448`, whose single remaining item is a deferred
  `@ExceptionHandler` in `GlobalExceptionHandler` — a file nothing here touches. No conflict, no
  parent plan needed.
- **The legacy backends and monoliths.** Frozen before-picture.
- **A materialized view or summary table.** Would need the schema change this plan deliberately avoids;
  the indexes in C4 are enough at demo scale.
- **A Pact contract for the new endpoints.** The component/contract layer is opt-in; separate work.
