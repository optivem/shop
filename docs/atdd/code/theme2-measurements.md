# Theme 2 — measurements

The "before" numbers for theme 2, *the database is barred from the work it does best*, taken against
`system/multitier/backend-clean-java` **before any of Chunks A/R/B/C changed a line of `src/main`**.

They exist because the theme's claim is that *the system pays for it in performance*, and a
demonstration that cannot show the before-numbers is asserting rather than demonstrating.

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
