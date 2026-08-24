# Language equivalents

`system/multitier/` carries the clean-architecture variant in Java today; `backend-clean-dotnet` and
`backend-clean-typescript` do not exist yet. Decisions taken in the Java implementation are recorded
here in language-neutral terms **before** the twins are written, so porting is one model into two
languages rather than three codebases changed after the fact.

A decision that only works in Java is not a decision. Each entry states the rule, then the shape it
takes in each language.

## Infrastructure failure signalling (gateways)

**Rule.** An external system failing to answer — a non-2xx it was not supposed to return, an IO
failure, a timeout, an interrupt — is its own failure class with its own exception type per gateway,
all under one base type, living in the **domain package beside the gateway ports** — not beside the
adapters that throw them. A port declares both halves of its contract: what it answers with, and how
it says it could not answer. While the base type lived in infrastructure, no layer permitted to catch
it was permitted to name it, so "the ERP is down" could only surface as the catch-all 500.

It is **never** signalled with the language's built-in "programmer error" exception. That type stays
reserved for genuine bugs and misconfiguration, and the distinction is the whole point: a network
timeout and a bug in our own code must not arrive at the handler looking identical.

Catch clauses are narrowed to the checked/expected IO and parse failures. A blanket catch-all
re-wraps the adapter's own gateway exception in a second one and buries the original status.

| | Java | .NET | TypeScript |
|---|---|---|---|
| Base type | `abstract class GatewayException extends RuntimeException` | `abstract class GatewayException : Exception` | `abstract class GatewayException extends Error` |
| Per-gateway types | `TaxGatewayException`, `ClockGatewayException`, `ErpGatewayException` | same names | same names |
| Location | `domain/gateways/` | `Domain/Gateways/` | `domain/gateways/` |
| **Not** this | `IllegalStateException` | `InvalidOperationException` | bare `Error` / `TypeError` |
| Narrow the catch to | `IOException`, `InterruptedException` | `HttpRequestException`, `TaskCanceledException`, `JsonException` | `TypeError` from `fetch`, `SyntaxError` from `json()` |

**Misconfiguration is not a gateway failure.** An unknown external-system mode is a bad
configuration value, so it keeps the programmer-error type (`IllegalStateException` /
`InvalidOperationException` / `Error`). Do not fold it into the gateway family.

**HTTP status → 502.** `GatewayException` maps to **502 Bad Gateway**, one handler for the whole
family, sitting above the catch-all so it wins. 502 rather than 503 because the base type covers all
three ways an upstream can fail us — unreachable, an error status, a body we could not read — and 503
would promise the caller that retrying soon helps, which we do not know. The response body is a fixed
string on the same rule as the 500 below: the exception message names the upstream URL and its
response body, and that belongs in the log only.

The distinction is the point of the whole family: a 500 tells whoever is paged to look at our code, a
502 tells them to look at the upstream's.

## The catch-all 500 response body

**Rule.** An unhandled exception is by definition something we did not mean to expose. The full
message and every cause go to the log at ERROR; the response body carries a fixed string that says
nothing about the server's internals. Exception messages routinely name internal classes, SQL, and
host addresses.

| | Java | .NET | TypeScript |
|---|---|---|---|
| Handler | `@ExceptionHandler(Exception.class)` | `IExceptionHandler` / exception-handling middleware | error middleware |
| Body | `ProblemDetail` with a constant `detail` | `ProblemDetails` with a constant `Detail` | problem-details JSON with a constant `detail` |
| Log | `log.error("...", ex)` — the stack trace carries the causes | `ILogger.LogError(ex, "...")` | `logger.error(err)` |

The `type`, `title`, `status`, and `timestamp` fields are unchanged; only the free-text detail is
fixed.

## Unlimited quantities are null, not a sentinel

**Rule.** "No usage limit" is modelled as an absent value all the way through — request DTO, domain
value object, persisted column, response DTO. No `MAX_VALUE`-style sentinel is written to storage.

| | Java | .NET | TypeScript |
|---|---|---|---|
| Type | `Integer` | `int?` | `number \| null` |
| **Not** this | `Integer.MAX_VALUE` | `int.MaxValue` | `Number.MAX_SAFE_INTEGER` / `2147483647` |

The legacy services (`system/monolith/*`, `system/multitier/backend-{java,dotnet,typescript}`) still
write the sentinel — that is the before-picture and is left alone. All four front-ends already render
both `null` and `2147483647` as "Unlimited", so the clean variant can drop the sentinel without a
front-end change.

## Theme 2 — the database does the work it is good at

**Rule.** The dogma being corrected is *"the database is a detail"* applied to **capability** instead
of to **coupling**. Decoupling from a dialect is not the same as refusing set operations. Every
decision below keeps the port and changes only what it *says* — no `Pageable`, no `DbContext`, no
`PrismaClient`, no SQL string ever appears in a port signature.

These were settled in `backend-clean-java` (Chunks A, R, B and C, 2026-08-18/20) and are recorded
here because `backend-clean-dotnet` and `backend-clean-typescript` do not exist yet. The measured
result of each is in [`theme2-measurements.md`](../../../system/multitier/backend-clean-java/docs/theme2-measurements.md).

### Intent-named ports, never mechanism-named ones

This is the headline and the only rule here that is purely about vocabulary. A port that names a
*mechanism* — `findAll()` — forces the loop into the application layer, because "give me every row"
is the only question it can be asked. A port that names an *intent* — `recallSku(sku)` — leaves the
adapter free to answer with one statement. The abstraction survives; only the loop dies.

| | Java (settled) | .NET | TypeScript |
|---|---|---|---|
| Set-based write | `int cancelOutstandingForSku(Sku sku)` | `Task<int> CancelOutstandingForSkuAsync(Sku sku)` | `cancelOutstandingForSku(sku: Sku): Promise<number>` |
| | `int deliverPlacedOlderThan(Instant cutoff)` | `Task<int> DeliverPlacedOlderThanAsync(DateTimeOffset cutoff)` | `deliverPlacedOlderThan(cutoff: Date): Promise<number>` |
| Adapter uses | JPQL bulk `UPDATE` | `ExecuteUpdateAsync` with a predicate | Prisma `updateMany` with a where-guard |
| **Not** this | `findAll()` + filter + `save` per row | `ToListAsync()` + `foreach` + `SaveChanges` | `findMany()` + `for` + `update` |

**The returned count is part of the contract, not a courtesy.** A bulk write that reports nothing
leaves the caller unable to tell "no rows matched" from "it worked", and that is the one thing the
loop version could always answer. Check it.

### Atomic read-modify-write is one conditional statement

**Rule.** Read-then-write across two statements is a lost update, not a style preference. Two orders
redeeming the same coupon both read `used=4` and both write `used=5`, and the usage limit silently
does not hold. The port returns whether it won.

| | Java (settled) | .NET | TypeScript |
|---|---|---|---|
| Port | `boolean tryRedeem(CouponCode code)` | `Task<bool> TryRedeemAsync(CouponCode code)` | `tryRedeem(code: CouponCode): Promise<boolean>` |
| Adapter | one `UPDATE … WHERE used_count < usage_limit`, rows-affected as the verdict | same statement via `ExecuteUpdateAsync` | same statement via `updateMany`, `count` as the verdict |

The Java version is pinned by `CouponRedemptionConcurrencyIntegrationTest`; each twin needs its own
concurrent test, because no single-threaded test can fail on this.

### The read model is its own port, and it does not go through the domain

**Rule.** A use case whose response holds **nothing the database does not already hold** is a pure
query: it reads a flat projection and never materialises an entity. The census test for any new use
case is exactly that question. If the response ever gains a computed field — a `redeemableNow` flag
is the standing example, since it is validity ∧ quota — the use case leaves this category and the
domain is back in the loop.

The second argument is stronger than performance and is the one to lead with: a write-side invariant
must not be able to break a read. A `Coupon` constructor that rejects a zero `discountRate` means one
bad row takes down the whole *list* endpoint with a 500. A read path that projects columns cannot
fail that way.

| | Java (settled) | .NET | TypeScript |
|---|---|---|---|
| Port location | `usecases/queries/` — **not** `domain/repositories/` | `UseCases/Queries/` | `usecases/queries/` |
| Ports | `SalesReportQuery`, `OrderQuery`, `CouponQuery` | `ISalesReportQuery`, `IOrderQuery`, `ICouponQuery` | same names, no `I` prefix |
| Projection | flat records — `OrderListItem`, `OrderDetail`, `CouponListItem` | records via `Select` + `AsNoTracking` | a Prisma `select` clause into a plain type |
| Enforced by | ArchUnit `READ_USECASES_DO_NOT_TOUCH_THE_DOMAIN` | an equivalent architecture test | an equivalent architecture test |

Placement is the part that is easy to get wrong. Putting these in the domain's repository package
would assert the report *is* domain — and the domain never calls it. The projection return type is
what bypasses the model; the placement is what makes the code admit it.

### Aggregation is the database's job

**Rule.** `GROUP BY` in the adapter, never a stream fold over two `findAll()`s. Three questions are
three statements, and the extra round trip is the honest price of not hydrating the table.

| Java (settled) | .NET | TypeScript |
|---|---|---|
| `List<RevenueByCountryMonth> revenueByCountryAndMonth()` | `Task<IReadOnlyList<RevenueByCountryMonth>>` | `revenueByCountryAndMonth(): Promise<RevenueByCountryMonth[]>` |
| `List<TopSkuByRevenue> topSkusByRevenue(int limit)` | `TopSkusByRevenueAsync(int limit)` | `topSkusByRevenue(limit: number)` |
| `List<CouponEffectiveness> couponEffectiveness()` | `CouponEffectivenessAsync()` | `couponEffectiveness()` |

The limit is validated in the use case (1..100, default 10) rather than in the adapter — the bound is
a contract, not a query detail.

### The transaction boundary is a port

**Rule.** A transaction is an infrastructure mechanism that the use case must nonetheless control.
Annotating the use case is banned by `USECASES_ARE_FRAMEWORK_FREE_EXCEPT_JAKARTA_VALIDATION`;
annotating the controller puts the boundary in the wrong layer. That is precisely what a port is for.

| Java (settled) | .NET | TypeScript |
|---|---|---|
| `<T> T inTransaction(Supplier<T> work)` over `TransactionTemplate` | `Task<T> InTransactionAsync<T>(Func<Task<T>>)` over `IDbContextTransaction` | `inTransaction<T>(work: () => Promise<T>): Promise<T>` over Prisma `$transaction` |

### Paging belongs to the read side, and is always keyset

**Rule.** The page vocabulary lives beside the query ports, not in the domain — nothing on the
command side pages, and in Java a domain placement fails the build outright. Keyset, never `OFFSET`:
an offset re-scans and skips, and it drops or repeats rows when the table is written to underneath a
paging client.

| | Java (settled) | .NET | TypeScript |
|---|---|---|---|
| Vocabulary | `usecases/queries/` — `PageSpec<C>`, `Page<T>`, `OrderCursor` | `UseCases/Queries/` — same three | `usecases/queries/` — same three |
| Defaults | `DEFAULT_SIZE = 50`, `MAX_SIZE = 200` | same values | same values |
| Fetch | `LIMIT size + 1`, so `hasMore` costs no second query | same | same |
| Wire | `size` + an opaque base64 `cursor`; decoded in `presentation/CursorCodec` | same, decoded in the presentation layer | same, decoded in the presentation layer |
| **Not** this | Spring `Pageable` / `PageRequest` in a port | `IQueryable`, EF `Skip`/`Take` in a port | Prisma `take`/`cursor` in a port |

**The sort key never reaches the client.** The cursor is base64-encoded at the presentation boundary
and decoded there, so no consumer learns that orders key on `(order_timestamp, order_number)` or that
coupons key on the surrogate `id`. That is what makes the ordering changeable later.

**Order newest-first, and know why.** Orders sort `order_timestamp DESC, order_number DESC`, so a
test's own order is always on page 1. Coupons have no timestamp column, so newest-published-first
comes from `ORDER BY id DESC` **inside the adapter** — the surrogate `id` never leaves infrastructure,
and the keyset cursor is the `UNIQUE`, domain-visible `code`. This matters more than it looks: the
system-test DSLs search only the returned list, so an ordering that puts the row under test on an
arbitrary page turns every list assertion into a paging loop.

## A benchmark probe asserts its own effect

**Rule.** The theme-2 harness (`src/benchmark`, `./gradlew benchmark`) asserts nothing by design — a
number that moved must never turn the build red. The cost of that choice is that it **can only fail
by throwing**, and a probe that measures nothing throws nothing: it reports a small number, which
reads as a win. So every probe returns a count of what it actually did, and the harness fails when a
count that must be positive comes back zero.

This is not hypothetical. Taking the after-numbers on 2026-08-20, the before-picture recall probe
reported 438 ms for 2,000 orders in **one** JDBC statement — a 21× improvement that would have gone
into `theme2-measurements.md` as fact. `Theme2Baseline` was comparing `RECALLED_SKU.equals(order.getSku())`,
and `Order.getSku()` had since been given the `Sku` value object, so a `String` was being compared to
a record. The filter matched nothing, the loop cancelled nothing, and the probe timed 100,000 orders
being hydrated for no reason. It compiled without a warning and the test passed.

**The trap is a primitive compared to a value object, and every language has its own version:**

| | Java | .NET | TypeScript |
|---|---|---|---|
| Silently false | `"SKU-007".equals(order.getSku())` — `equals(Object)` accepts anything | `"SKU-007".Equals(order.Sku)` — the `object` overload, no compile error | `'SKU-007' === order.sku` where `Sku` is a class |
| Caught at compile time | — | `==` between `string` and `Sku` | the same comparison — TS2367, "no overlap" |
| Correct | `.equals(order.getSku().value())` | `.Equals(order.Sku.Value)` | `=== order.sku.value` |

Note the asymmetry, because it decides where each twin needs the guard most. In .NET the operator
form is a compile error while the `Equals` form is not, so the trap survives exactly one refactor away
from safety. In TypeScript the shape of `Sku` decides everything: a **class** makes the comparison a
compile error, while a **branded `string`** is a plain string at runtime, so it compares *correctly* —
and that is its own hazard, since structural typing then lets a raw column value flow into a
`Sku`-typed slot with no boundary crossed and nothing to catch. Type safety catches this in some
shapes and not in others, which is exactly why the guard cannot be the type system.

**The guard, which is language-independent.** Every probe already returns the count it produced —
rows cancelled, groups aggregated, coupons redeemed. The harness fails the run when a probe that must
change rows reports zero. The check that actually caught this one was arithmetic and belongs in the
harness rather than in a reviewer's head: *2,000 rows cannot be changed by a loop that issues one
statement in total.*

Same rule for the twins' benchmark harnesses when they are written: measure what you claim to
measure, and let the harness say so.
