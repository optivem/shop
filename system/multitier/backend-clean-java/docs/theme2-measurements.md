# Theme 2 — measurements

The "before" numbers for theme 2, *the database is barred from the work it does best*, taken against
`system/multitier/backend-clean-java` **before any of Chunks A/R/B/C changed a line of `src/main`**.

They exist because the theme's claim is that *the system pays for it in performance*, and a
demonstration that cannot show the before-numbers is asserting rather than demonstrating.

> **Superseded, 2026-08-25 — Chunk C's paging mechanism.** The read side no longer pages by keyset
> cursor. `PageSpec`/`Page` now carry a page number and a total, the adapters use
> `LIMIT … OFFSET …` plus a `COUNT(*)`, and `CursorCodec`, `OrderCursor` and
> `KeysetPagingIntegrationTest` are gone — replaced by `OffsetPagingIntegrationTest`. The reason is
> not performance: numbered pages with a total are the paging convention readers already know, and
> "page 3 of 26" cannot be built from a resume token. Everything else in this document stands, and
> the Chunk C numbers below stand as measured — they record what keyset paging cost on the day it
> ran, which is precisely what makes the trade visible. What changed is the answer to *is that the
> paging this application should ship*, not the arithmetic. Read every "keyset" below as the
> mechanism that was measured, not the one in `src/main` today — and see the 2026-08-25 section
> at the foot of this document for the same operations measured on numbered pages.

## How to reproduce

```bash
cd system/multitier/backend-clean-java
./gradlew benchmark
```

The task starts a Postgres 16 container, applies `system/db/migrations`, loads
`system/db/seed/demo-volume.sql` (100,000 orders, 300 coupons, deterministic — no `random()`), then
drives the **real** use case beans through the **real** ports and adapters. It writes
`build/benchmark/theme2-baseline.md` and echoes the same content to the console. The harness lives in
`src/benchmark` and asserts nothing: it can only fail by throwing, so a number that moved is never a
red build.

Where a capability has no call site yet — a bulk recall, an aggregate report — the harness writes the
honest in-memory alternative using only what the ports offer today. That code is the before-picture
and it stays out of `src/main` because nobody should ship it.

**After each chunk lands, re-run the same task and append its table below.** Same code, same seed,
same columns: that is the only reason the comparison means anything.

## Baseline — 2026-08-18, before Chunk A

Postgres 16-alpine under Testcontainers (Docker 29.5.2), JVM heap fixed at 2 GB, one developer
machine. Absolute milliseconds will differ elsewhere; the ratios are what the talk uses.

| Capability | Operation | Wall ms | Rows | Domain objects | JDBC statements | Retained heap MB |
|---|---|---:|---:|---:|---:|---:|
| Filtering, sorting, limiting | `BrowseOrderHistory` with no filter | 1158 | 100000 | 1130000 | 1 | 27 |
| Filtering, sorting, limiting | `BrowseOrderHistory` filtered on `DEMO-ORD-0500` | 37 | 100 | 1130 | 1 | 0 |
| Filtering, sorting, limiting | `BrowseCoupons` | 9 | 300 | 1500 | 1 | 0 |
| Projecting only the columns asked for | `ViewOrderDetails` × 1000 | 2559 | 1000 | 12000 | 1000 | 0 |
| Aggregation and joins | Three reports, in Java, over two `findAll()`s | 1217 | 529 | 1101500 | 2 | 0 |
| Atomic read-modify-write | Read-modify-write, 100 coupons | 798 | 100 | 500 | 400 | 0 |
| Set-based writes | Recall `SKU-007`: `findAll()` + filter + one `save` per order | 9093 | 2000 | 1100000 | 6001 | 0 |

Domain-object counts are derived from the mappers, so they are exact rather than sampled. JDBC
statement counts come from Hibernate's own statistics rather than from an estimate of how many round
trips a loop *should* make. Retained heap is a `Runtime` delta after a GC hint and is indicative
only. Every read path is run once untimed before it is measured, so no row is quoting a cold start —
the un-warmed first `BrowseCoupons` took 866 ms for 300 rows whose plan executes in 0.03 ms, which is
a real number and the wrong one.

## What the numbers say

**The database is not the slow part. It is barely doing anything.**

| | The database's share | The application's share |
|---|---:|---:|
| `BrowseOrderHistory`, unfiltered | 48 ms | 1110 ms |
| `BrowseCoupons` | 0.03 ms | 9 ms |
| `ViewOrderDetails`, per read | 0.03 ms | 2.53 ms |

- **`BrowseOrderHistory` spends 96% of its wall time outside Postgres.** One statement, 48 ms of
  execution, and then 1.13 million domain objects are constructed — six `Money`, two `Rate`, an
  `OrderPricing`, a `Country` and an `Order` per row — every one of which is unwrapped back into a
  primitive on the next line and is unreachable by the time the response is returned. The 27 MB the
  response still holds is the *response DTOs*; the domain model that produced them is already
  garbage. That gap between 1,130,000 constructed and 0 retained is Chunk R's whole argument.

- **A single-row read costs 2.56 ms, of which the database is 0.029 ms — an 88× multiplier.**
  `ViewOrderDetails` returns fifteen fields, every one a stored column, and reaches them by building
  a whole `Order` first. There is nothing to optimise in the query; there is an entire object graph
  to not build.

- **The unbounded sort spills to disk.** `Sort Method: external merge  Disk: 12048kB` — with no
  `LIMIT`, Postgres cannot use a top-N sort, so it writes 12 MB of temp files on every page load.
  This is what `idx_orders_recent` plus keyset paging (Chunk C) removes, and it is visible in
  `EXPLAIN` rather than inferred.

- **Bulk recall costs 6,001 statements to change 2,000 rows.** One `findAll()` that hydrates the
  entire table, then three statements per order (`findByOrderNumber`, the merge's `SELECT`, the
  `UPDATE`). 9.1 seconds. The planner's own answer to the same question is one `Seq Scan` behind one
  `Update` node — the last plan below. This is the headline, and the cause is the port's vocabulary:
  `OrderRepository` can only say *"give me all the rows"*, so the filter has to be a Java `filter`.

- **Coupon redemption is four statements per redemption and it is still wrong.** `findByCode`, the
  adapter's key lookup, the merge's `SELECT`, the `UPDATE` — 8 ms each, and between the read and the
  write another request can redeem the same last unit. The cost is the small half of this row; the
  lost update is the large half, and Chunk A4's concurrency test is what makes it real rather than
  theoretical.

- **The filtered browse is the interesting non-problem.** 37 ms wall against 35 ms of execution: the
  `LIKE` genuinely is pushed down, so the application is not the bottleneck here. But the plan reads
  `Rows Removed by Filter: 99900` on a sequential scan, and nothing bounds the result — a filter that
  matched 50,000 rows would return 50,000 rows. Pushed down is not the same as bounded, and that
  distinction is Chunk C.

## Asides worth keeping

- **`SELECT * FROM coupons WHERE code = …` is a sequential scan** even though `code` is `UNIQUE` and
  therefore indexed. At 300 rows Postgres correctly judges the scan cheaper than the index. It is a
  useful reminder mid-talk that "the database is slow" and "add an index" are both reflexes, and
  neither is what this theme is about.

- **Two of these capabilities have no call site at all.** There is no bulk write and no report in
  `src/main` today, so their before-numbers had to be written specially for this harness. That
  absence is itself a finding: the codebase does not do these things slowly, it does not do them.

## Query plans

Reads are `EXPLAIN (ANALYZE, BUFFERS)` — actually executed. The write is plain `EXPLAIN`, planned but
not run, so it cannot disturb the data the other measurements were taken over.

### `BrowseOrderHistory`, unfiltered

```
Sort  (cost=16903.32..17153.32 rows=100000 width=111) (actual time=31.576..43.948 rows=100000 loops=1)
  Sort Key: order_timestamp DESC
  Sort Method: external merge  Disk: 12048kB
  Buffers: shared hit=1785, temp read=1506 written=1510
  ->  Seq Scan on orders  (cost=0.00..2785.00 rows=100000 width=111) (actual time=0.005..5.832 rows=100000 loops=1)
        Buffers: shared hit=1785
Planning Time: 0.033 ms
Execution Time: 48.426 ms
```

### `BrowseOrderHistory`, filtered

```
Sort  (cost=3285.17..3285.19 rows=10 width=111) (actual time=34.961..34.966 rows=100 loops=1)
  Sort Key: order_timestamp DESC
  Sort Method: quicksort  Memory: 38kB
  Buffers: shared hit=1785
  ->  Seq Scan on orders  (cost=0.00..3285.00 rows=10 width=111) (actual time=16.243..34.933 rows=100 loops=1)
        Filter: (lower((order_number)::text) ~~ '%demo-ord-0500%'::text)
        Rows Removed by Filter: 99900
        Buffers: shared hit=1785
Planning Time: 0.046 ms
Execution Time: 35.004 ms
```

### `BrowseCoupons`

```
Seq Scan on coupons  (cost=0.00..7.00 rows=300 width=51) (actual time=0.005..0.019 rows=300 loops=1)
  Buffers: shared hit=4
Planning Time: 0.026 ms
Execution Time: 0.033 ms
```

### `ViewOrderDetails`

```
Index Scan using orders_order_number_key on orders  (cost=0.42..8.44 rows=1 width=111) (actual time=0.017..0.018 rows=1 loops=1)
  Index Cond: ((order_number)::text = 'DEMO-ORD-050000'::text)
  Buffers: shared hit=4
Planning Time: 0.033 ms
Execution Time: 0.029 ms
```

### Coupon lookup by code

```
Seq Scan on coupons  (cost=0.00..7.75 rows=1 width=51) (actual time=0.018..0.025 rows=1 loops=1)
  Filter: ((code)::text = 'DEMO-CPN-0001'::text)
  Rows Removed by Filter: 299
  Buffers: shared hit=4
Planning Time: 0.026 ms
Execution Time: 0.030 ms
```

### The rows a recall of `SKU-007` has to find

```
Seq Scan on orders  (cost=0.00..3035.00 rows=2007 width=111) (actual time=0.005..7.690 rows=2000 loops=1)
  Filter: ((sku)::text = 'SKU-007'::text)
  Rows Removed by Filter: 98000
  Buffers: shared hit=1785
Planning Time: 0.025 ms
Execution Time: 7.761 ms
```

### What Chunk A will issue instead (planned, not executed)

```
Update on orders  (cost=0.00..3285.00 rows=0 width=0)
  ->  Seq Scan on orders  (cost=0.00..3285.00 rows=1805 width=124)
        Filter: (((status)::text <> 'CANCELLED'::text) AND ((sku)::text = 'SKU-007'::text))
```

## After — 2026-08-20, Chunks A, R, B and C landed

Same task, same seed, same machine, same harness — `./gradlew benchmark` after `POST /api/admin/*`
(A1, A2), `tryRedeem` (A3, A4), the projection read paths (R1–R6), `SalesReportQuery` (B1–B5) and
keyset paging (C1–C5) were all in `src/main`.

`./gradlew integrationTest` was run first, in the same Docker session: 49 tests, all passing,
including the five cases of `KeysetPagingIntegrationTest`, which had never been executed before.
Chunk C's keyset SQL is hand-written and native, so the first-page numbers below would be worth
nothing if nobody had proved the cursor walks every row exactly once. (That test no longer exists —
see the superseded note at the top. Its successor, `OffsetPagingIntegrationTest`, proves the same
property about the paging that replaced it.)

| Capability | Operation | Wall ms | Rows | Domain objects | JDBC statements | Retained heap MB |
|---|---|---:|---:|---:|---:|---:|
| Filtering, sorting, limiting | `BrowseOrderHistory` with no filter, first page | 5 | 50 | 0 | 1 | 0 |
| Filtering, sorting, limiting | `BrowseOrderHistory` filtered on `DEMO-ORD-0500`, first page | 38 | 50 | 0 | 1 | 0 |
| Filtering, sorting, limiting | `BrowseCoupons`, first page | 19 | 50 | 0 | 1 | 0 |
| Projecting only the columns asked for | `ViewOrderDetails` × 1000 | 1243 | 1000 | 0 | 1000 | 0 |
| Aggregation and joins | Three reports, in Java, over two `findAll()`s | 1330 | 529 | 1101500 | 2 | 0 |
| Aggregation and joins | Three reports, three `GROUP BY`s | 165 | 529 | 0 | 3 | 0 |
| Atomic read-modify-write | Read-modify-write, 100 coupons | 853 | 100 | 500 | 400 | 0 |
| Atomic read-modify-write | `tryRedeem`, 100 coupons | 278 | 80 | 0 | 100 | 0 |
| Set-based writes | Recall `SKU-007`: `findAll()` + filter + one `save` per order | 8041 | 2000 | 1100000 | 6001 | 0 |
| Set-based writes | Recall `SKU-008`: one `UPDATE … WHERE` | 35 | 2000 | 0 | 1 | 0 |

Rows that appear twice are a pair: the before-picture is re-measured **in the same process, on the
same seed, in the same run** as the after, so the comparison never spans two machines or two JVMs.
The in-memory halves live in `src/benchmark` precisely because they must not ship.

### The three read rows changed what they measure, and that is the demonstration

Before C2, `BrowseOrderHistory` had no way to ask for less than everything; after it, the unbounded
read is not a request the port can express. So the three read rows now report the **first page at the
default size of 50** where the baseline reported the whole table:

| | Baseline | After | |
|---|---:|---:|---|
| `BrowseOrderHistory`, unfiltered | 1158 ms, 100000 rows, 1130000 objects | 5 ms, 50 rows, 0 objects | first page vs whole table |
| `BrowseCoupons` | 9 ms, 300 rows, 1500 objects | 19 ms, 50 rows, 0 objects | 300 rows fit in one page before |
| `ViewOrderDetails` × 1000 | 2559 ms, 12000 objects | 1243 ms, 0 objects | same work, 2.1× |

Comparing "100k rows" against "50 rows" is not a discrepancy to apologise for — it is the point, and
the harness labels the rows "first page" so nobody reads it as like-for-like. Two rows need saying
out loud rather than glossing:

- **`BrowseCoupons` got slower in wall time** — 9 ms to 19 ms — because at 300 rows there was never a
  paging problem to solve, and the keyset query pays for a subselect on `code` that the unbounded
  scan did not. Nothing was won here on time; the 1500 domain objects that are now 0 are this row's
  whole improvement, and the endpoint can no longer be handed a million coupons later.
- **The filtered read is unindexed and stays that way.** `lower(order_number) LIKE '%demo-ord-0500%'`
  has a leading wildcard, so no B-tree helps it; the plan is still a sequential scan removing 99,900
  rows, and 38 ms is the cost of that scan, not of the paging. C4 deliberately did not add a trigram
  index — substring search is not one of the six capabilities this theme is about.

`ViewOrderDetails` is the clean row: identical work, identical statement count, and 12,000 fewer
domain objects for a 2.1× wall-time drop. Fifteen fields that were reached by constructing seven
`Money`, two `Rate`, a `Country` and a `CouponCode` are now read as the columns they always were.

### The pairs, where before and after are the same question

| Capability | The application layer's way | The database's way | Wall | Statements | Domain objects |
|---|---|---|---|---|---|
| Set-based writes | `findAll()` + filter + `save` per order | one `UPDATE … WHERE` | 8041 ms → 35 ms (**230×**) | 6001 → 1 | 1100000 → 0 |
| Aggregation and joins | two `findAll()`s + three stream folds | three `GROUP BY`s | 1330 ms → 165 ms (**8×**) | 2 → 3 | 1101500 → 0 |
| Atomic read-modify-write | read, mutate, write back | one conditional `UPDATE` | 853 ms → 278 ms (**3×**) | 400 → 100 | 500 → 0 |

Both aggregation rows return exactly **529 groups** — 219 country/month pairs, 10 SKUs, 300 coupons —
because the probe pins the top-SKU limit to the whole catalogue. Same answer, one twentieth of the
memory traffic, and the extra statement is the honest cost: three questions are three `GROUP BY`s,
not two `findAll()`s.

The atomic row is the one where wall time is the least interesting column. 400 statements become 100
because the read half of every read-modify-write is gone, but the reason `tryRedeem` exists is that
the 400-statement version is *wrong* under concurrency — two orders on the same coupon both read
`used=4` and both write `used=5`. `CouponRedemptionConcurrencyIntegrationTest` is where that is
pinned; no number in this table can show it. Note also that the before-row accepts 100 of 100
attempts and the after-row 80 of 100: the extra 20 are coupons already at their usage limit, which
the read-modify-write loop incremented anyway. That gap is not a performance figure, it is the bug.

### The query plans moved too

The plan probes issue the same ad-hoc SQL as the baseline did, so they stay comparable — the
endpoints themselves no longer issue an unbounded `SELECT` at all.

**The unbounded sort no longer spills to disk.** Baseline: `Sort Method: external merge  Disk:
12048kB`, 48.4 ms, `temp read=1506 written=1510`. After C4:

```
Index Scan using idx_orders_recent on orders  (cost=0.42..9193.92 rows=100000 width=111) (actual time=0.037..21.347 rows=100000 loops=1)
  Buffers: shared hit=2672
Planning Time: 0.071 ms
Execution Time: 27.689 ms
```

No sort node, no temp files — the index supplies the order. 12 MB of writes per page load, gone.

**The recall's rows are found by index rather than by scanning the table.** Baseline: `Seq Scan on
orders`, 7.76 ms, 98,000 rows removed by filter. After C4:

```
Bitmap Heap Scan on orders  (cost=28.18..1926.02 rows=2050 width=111) (actual time=0.308..1.265 rows=2000 loops=1)
  Recheck Cond: ((sku)::text = 'SKU-007'::text)
  ->  Bitmap Index Scan on idx_orders_sku_status  (cost=0.00..27.67 rows=2050 width=0) (actual time=0.169..0.169 rows=2000 loops=1)
```

5.8× on the read, and the `UPDATE … WHERE` the recall actually issues uses the same index.

### A number that was wrong until it was checked

The first run of this benchmark reported the old-path recall at **438 ms, 0 rows, 1 statement** — a
21× "win" that would have gone into this document as fact. It was a harness bug: `Theme2Baseline`
compared `RECALLED_SKU.equals(order.getSku())`, and `Order.getSku()` returns the `Sku` value object
introduced after the baseline was taken, so a `String` was being compared to a record and the filter
matched nothing. The probe hydrated 100,000 orders, cancelled none, and timed an empty loop.

It is recorded here rather than quietly fixed because it is the failure mode this document exists to
guard against: the harness asserts nothing, so it can only fail by throwing, and a probe that
measures nothing throws nothing. The check that caught it was arithmetic, not tooling — 2,000 rows
cannot be updated by a loop that issues one `save` per row and one statement in total.

## Re-measured — 2026-08-20, after `save` split into `add` and `update`

The two sections above are left exactly as they were taken: each was accurate for the code that
produced it. This one supersedes the pair ratios, because the before-picture itself got cheaper.

`OrderRepository` and `CouponRepository` no longer expose a single `save`. They expose `add` and
`update`, because no caller was ever unsure which it meant — `PlaceOrder` has just minted an order
number that cannot already exist, `CancelOrder` is holding a row it read a moment ago. `save` threw
that knowledge away and paid the database to recover it: an adapter-level lookup by natural key
before every write, purely to learn the surrogate id. Removing it takes **one statement off every
write in the application**, including both in-memory loops this document measures.

| Capability | Operation | Wall ms | Rows | Domain objects | JDBC statements | Retained heap MB |
|---|---|---:|---:|---:|---:|---:|
| Filtering, sorting, limiting | `BrowseOrderHistory` with no filter, first page | 5 | 50 | 0 | 1 | 0 |
| Filtering, sorting, limiting | `BrowseOrderHistory` filtered on `DEMO-ORD-0500`, first page | 35 | 50 | 0 | 1 | 0 |
| Filtering, sorting, limiting | `BrowseCoupons`, first page | 5 | 50 | 0 | 1 | 0 |
| Projecting only the columns asked for | `ViewOrderDetails` × 1000 | 1176 | 1000 | 0 | 1000 | 0 |
| Aggregation and joins | Three reports, in Java, over two `findAll()`s | 876 | 529 | 1101500 | 2 | 0 |
| Aggregation and joins | Three reports, three `GROUP BY`s | 130 | 529 | 0 | 3 | 0 |
| Atomic read-modify-write | Read-modify-write, 100 coupons | 468 | 100 | 500 | 200 | 0 |
| Atomic read-modify-write | `tryRedeem`, 100 coupons | 220 | 80 | 0 | 100 | 0 |
| Set-based writes | Recall `SKU-007`: `findAll()` + filter + one `update` per order | 3638 | 2000 | 1100000 | 2001 | 0 |
| Set-based writes | Recall `SKU-008`: one `UPDATE … WHERE` | 31 | 2000 | 0 | 1 | 0 |

### The pairs, restated

| Capability | The application layer's way | The database's way | Wall | Statements | Domain objects |
|---|---|---|---|---|---|
| Set-based writes | `findAll()` + filter + `update` per order | one `UPDATE … WHERE` | 3638 ms → 31 ms (**117×**) | 2001 → 1 | 1100000 → 0 |
| Aggregation and joins | two `findAll()`s + three stream folds | three `GROUP BY`s | 876 ms → 130 ms (**7×**) | 2 → 3 | 1101500 → 0 |
| Atomic read-modify-write | read, mutate, write back | one conditional `UPDATE` | 468 ms → 220 ms (**2×**) | 200 → 100 | 500 → 0 |

The arithmetic that changed, stated plainly so the earlier sections can be read against it:

- **Bulk recall is two statements per order, not three.** `findByOrderNumber`, the merge's `SELECT`
  and the `UPDATE` become the `UPDATE` alone plus the loop's own read: 6,001 statements to 2,001.
- **Coupon redemption is two statements per redemption, not four.** `findByCode` and one `UPDATE`,
  where it was `findByCode`, the adapter's key lookup, the merge's `SELECT` and the `UPDATE`: 400
  statements to 200.
- **The set-based ratio fell from 230× to 117×, and the aggregation and atomic ratios moved with
  it.** Nothing regressed; the loop got faster.

**A shrinking ratio is the honest outcome, and it is worth saying why rather than quoting the older,
larger number.** The comparison was never "the database versus Java" — it was always "the database
versus the best loop you would actually write". A loop that re-reads every row before writing it is
not that loop; it is a loop with a bug in its repository, and half of the 230× was measuring the bug
rather than the architecture. 117× is what the argument is actually worth, and it survives at that
size: 2,000 statements are still 2,000 network round trips that one `UPDATE … WHERE` does not make,
and 1.1 million domain objects are still constructed to answer a question that needs none.

The lesson runs the other way too, and it belongs in the talk. The before-picture is code, and code
can be wrong in ways that flatter the after-picture. Any number in this document that gets larger
because the naive path is badly written is a number that will shrink the moment someone writes the
naive path properly — so the durable claims are the ones in the statement and object columns, which
are structural, rather than the wall-clock ratios, which are not.

## A plan that explained a query nobody ran — 2026-08-25

The `What ... will issue instead (planned, not executed)` blocks above were generated by
`probe.explainOnly` over this string:

```sql
UPDATE orders SET status = 'CANCELLED' WHERE sku = 'SKU-007' AND status <> 'CANCELLED'
```

`cancelOutstandingForSku` — the set-based recall those blocks claim to describe — issues
`WHERE ... AND o.status = :placed`. So the recorded plans explained a clause the application has
never sent. The harness now explains `status = 'PLACED'`, matching the query it stands for.

The plans above are left exactly as they were taken, per this document's rule that each section is
accurate for the code that produced it. **No measurement in this document changes.** The naive
recall loop's guard was tightened in the same edit, from `status != CANCELLED` to `status == PLACED`,
and on this seed the two select the same 2,000 rows: `SKU-007` means `n % 50 == 6`, which forces
`n % 10 == 6`, which `demo-volume.sql` maps to `PLACED`. There is no `DELIVERED` `SKU-007` order for
the two guards to disagree about, so every row count, statement count and timing stands.

It is recorded rather than quietly fixed for the same reason as the section above: the harness
asserts nothing about its own plans, so a `WHERE` clause that drifts from the code it illustrates
fails silently and reads as evidence. The tightened guard now also fails loudly if the seed ever
changes — `Order.cancel()` refuses a non-`PLACED` order by throwing, so a `DELIVERED` row in the
recall set would end the run instead of being counted as cancelled.

## Re-measured — 2026-08-25, after numbered paging, and after the harness could run again

The sections above are left exactly as they were taken. This one supersedes their read rows, for two
reasons that arrived together.

**The harness had been dead for five days.** `Theme2Baseline` autowired `BrowseOrderHistory`,
`BrowseCoupons`, `ViewOrderDetails` and `ViewSalesReport` by their concrete types. Once
`UseCaseConfig` began publishing every use case wrapped — logging outside, refusal translation
inside — no bean of those types existed, the Spring context failed to build, and `./gradlew
benchmark` never reached a measurement. So `build/benchmark/theme2-baseline.md` sat at its
2026-08-20 contents while three chunks of `src/main` moved underneath it, and every number quoted
from it in the interim was quoting code that had already changed. A benchmark nobody runs is a
benchmark nobody notices is broken.

The fix asks for the port — `UseCase<Request, Response>` — which is what the container holds and what
the controller calls. The decorators' cost is therefore **inside** these numbers now, where it was
outside them before. That is the honest place for it: nothing in production calls a naked use case
either.

**Paging changed mechanism.** Keyset cursors became numbered pages, so a page now costs two
statements rather than one — the page itself, and the `COUNT(*)` that a page number cannot be
computed without. That is the one difference below that is structural rather than machine noise, and
it is the price of the total; see the superseded note at the top of this document.

| Capability | Operation | Wall ms | Rows | Domain objects | JDBC statements | Retained heap MB |
|---|---|---:|---:|---:|---:|---:|
| Filtering, sorting, limiting | `BrowseOrderHistory` with no filter, first page | 12 | 50 | 0 | 2 | 0 |
| Filtering, sorting, limiting | `BrowseOrderHistory` filtered on `DEMO-ORD-0500`, first page | 73 | 50 | 0 | 2 | 0 |
| Filtering, sorting, limiting | `BrowseCoupons`, first page | 15 | 50 | 0 | 2 | 0 |
| Projecting only the columns asked for | `ViewOrderDetails` × 1000 | 1353 | 1000 | 0 | 1000 | 0 |
| Aggregation and joins | Three reports, in Java, over two `findAll()`s | 1236 | 529 | 1101500 | 2 | 0 |
| Aggregation and joins | Three reports, three `GROUP BY`s | 176 | 529 | 0 | 3 | 0 |
| Atomic read-modify-write | Read-modify-write, 100 coupons | 574 | 100 | 500 | 200 | 0 |
| Atomic read-modify-write | `tryRedeem`, 100 coupons | 280 | 80 | 0 | 100 | 0 |
| Set-based writes | Recall `SKU-007`: `findAll()` + filter + one `update` per order | 3621 | 2000 | 1100000 | 2001 | 0 |
| Set-based writes | Recall `SKU-008`: one `UPDATE … WHERE` | 31 | 2000 | 0 | 1 | 0 |

### The pairs, restated

| Capability | The application layer's way | The database's way | Wall | Statements | Domain objects |
|---|---|---|---|---|---|
| Set-based writes | `findAll()` + filter + `update` per order | one `UPDATE … WHERE` | 3621 ms → 31 ms (**117×**) | 2001 → 1 | 1100000 → 0 |
| Aggregation and joins | two `findAll()`s + three stream folds | three `GROUP BY`s | 1236 ms → 176 ms (**7×**) | 2 → 3 | 1101500 → 0 |
| Atomic read-modify-write | read, mutate, write back | one conditional `UPDATE` | 574 ms → 280 ms (**2×**) | 200 → 100 | 500 → 0 |

### Read the ratios, not the absolutes

Every wall-clock figure in this run is up roughly a fifth against 2026-08-20 — the aggregation
before-row moved 876 ms to 1236 ms, `ViewOrderDetails` 1176 ms to 1353 ms — on the same seed, the
same statements and the same object counts. Whatever that is, it is not the code: no change between
the two runs touched the recall loop or the report folds. It is the machine, the container, the JIT,
or all three.

The three pair ratios are unmoved at **117×**, **7×** and **2×**, and the statement and object
columns are identical to the row. That is the whole argument for reading this document by its
structural columns: 2001 statements to 1 is a fact about the architecture, and 3621 ms is a fact
about a laptop on a Tuesday.

Two read rows are the exception, and both are explained rather than noise: `BrowseOrderHistory` and
`BrowseCoupons` each went from one statement to two, which is numbered paging asking for its total.
`BrowseCoupons` moved 5 ms to 15 ms, and at 300 rows a `COUNT(*)` is most of that — the smallest
table pays the highest proportional price for a page number, which is worth knowing before this
vocabulary is copied onto a list that never needed paging at all.
