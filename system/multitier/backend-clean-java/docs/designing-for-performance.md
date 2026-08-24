# Designing for Performance

Where business logic should live — the application layer or the database — and how to use a
database effectively without letting it back into the core.

Examples come from the same pair as the other design notes:

| | Path |
|---|---|
| **Before** — logic in the app, rows in memory | [`system/multitier/backend-java`](../../backend-java) |
| **After** — set operations in the adapter | [`system/multitier/backend-clean-java`](..) — this project |

Related: [Decoupling the Domain from the ORM](decoupling-domain-from-orm.md) ·
[Decoupling from External Systems](decoupling-from-external-systems.md)

---

## The thesis

From the comment at the top of `JpaSalesReportQuery`:

> Native rather than JPQL, and deliberately so. `date_trunc` is Postgres-specific, and that is fine
> here in a way it would never be one layer up: **an adapter is the layer whose job is to know which
> database it is talking to.** Decoupling the application from the database's *dialect* is not the
> same as refusing the database's *set operations* — confusing the two is what pushed this work into
> memory in the first place.

That is the reframe the whole page hangs on. Teams hear "decouple from the database" and stop using
the database. The decoupling is what gives you a *place* to be database-specific — one adapter —
which is precisely what makes the fast version safe.

A second, blunter framing: **most performance bugs are API design bugs wearing a performance
costume.** Every anti-pattern below started as a port that named a mechanism instead of a question.

---

## Anti-pattern 1 — Unbounded read, then filter in memory

**Before** — `backend-java/.../core/services/OrderService.java`:

```java
public BrowseOrderHistoryResponse browseOrderHistory(String orderNumberFilter) {
    List<Order> orders;
    if (orderNumberFilter == null || orderNumberFilter.trim().isEmpty()) {
        orders = orderRepository.findAllByOrderByOrderTimestampDesc();   // <- every row. ever.
    } else {
        orders = orderRepository.findByOrderNumberContainingIgnoreCaseOrderByOrderTimestampDesc(...);
    }
    var items = orders.stream().map(order -> { /* copy 8 fields */ }).toList();
    // ...
}
```

**Why it happened** — the port named a mechanism, so it could only be asked one question. From the
comment on `usecases/queries/OrderQuery.java`:

> this port used to be `findAllByOrderByOrderTimestampDesc()` on the domain repository — Spring
> Data's query-derivation DSL spelled into a port whose own javadoc claimed "no Spring Data, no
> JPA". Because the port named a mechanism, "give me all the rows" was the only question it could
> be asked, so filtering and limiting had nowhere to happen but in memory.

**After** — the port takes the filter *and* the page, so both stay in SQL:

```java
public interface OrderQuery {
    Page<OrderListItem> listOrders(String orderNumberFilter, PageSpec<OrderCursor> page);
    Optional<OrderDetail> findOrderDetail(String orderNumber);
}
```

The `if` in the service existed because the interface could not express the question.

---

## Anti-pattern 2 — `OFFSET` paging

The obvious fix to an unbounded read is `Pageable` / `OFFSET`. That is a trap.
`infrastructure/persistence/queries/JpaOrderQuery.java`:

```java
private static final String KEYSET_PREDICATE =
        "(o.order_timestamp, o.order_number) < (:cursorTimestamp, :cursorOrderNumber)";

private static final String LIST_ORDER = """
        ORDER BY o.order_timestamp DESC, o.order_number DESC
        LIMIT :limit
        """;
```

> Row-value comparison, and not OFFSET. `(a, b) < (x, y)` is one comparison Postgres can satisfy by
> descending `idx_orders_recent` straight to the resume point; `OFFSET 10000` makes it read ten
> thousand rows in order to throw them away, and that cost grows with the page number while this one
> does not. The columns and their order have to match the ORDER BY exactly or the index does not
> apply — which is why the tuple is spelled as a tuple rather than expanded into
> `ts < :ts OR (ts = :ts AND num < :num)`, a form that means the same thing and plans worse.

Three supporting details.

### a) `hasMore` without a second `COUNT(*)`

```java
.setParameter(LIMIT, page.size() + 1);        // ask for one more row than you need
// ...
var hasMore = rows.size() > page.size();
var onThisPage = hasMore ? rows.subList(0, page.size()) : rows;
```

From `usecases/queries/Page.java`:

> one row cheaper than a second `COUNT(*)` over the same predicate, and it is the only extra fact a
> cursor-paged client needs: there is no total here, because a keyset page never knows how many
> pages follow it — and counting them would re-read everything the paging was introduced to stop
> reading.

### b) The page ceiling is a use-case rule, not an adapter rule

`usecases/queries/PageSpec.java`:

```java
public static final int DEFAULT_SIZE = 50;
public static final int MAX_SIZE = 200;
```

> The ceiling exists because "how many rows may one request cost?" is a question the caller is not
> allowed to answer. Without it, `?size=1000000` is the unbounded read all over again, wearing a
> page's clothes.

Enforced in `BrowseOrderHistory`, not in the adapter:

```java
if (!PageSpec.isValidSize(request.size())) {
    return Result.err(new UseCaseError.Invalid(FIELD_SIZE,
            "Page size must be between 1 and " + PageSpec.MAX_SIZE));
}
```

> That bound is the use case's to enforce and nobody else's: the adapter would honour any number it
> is handed, and the controller is the layer that has just been told a number by a stranger.

### c) `PageSpec` is hand-written rather than Spring's `Pageable`

> A port that names a framework type has imported the framework into the layer that was supposed to
> be free of it — and `Pageable` brings OFFSET with it, which is the mechanism this vocabulary
> exists to avoid.

And the cursor type is deliberate too — `usecases/queries/OrderCursor.java`:

> The tiebreaker is the order number rather than the surrogate id. Textbook keyset pagination
> reaches for the primary key, and this codebase has no primary key to reach for — a domain `Order`
> carries no `Long id`. The constraint forces the honest version: a cursor is handed to a client, so
> its key has to be a column that is unique, stable, and already public.

Correctness here is not something the compiler can check, so
`integrationTest/.../KeysetPagingIntegrationTest` drives real Postgres and pins the property that
matters:

> walking the pages must visit every row exactly once. A cursor that is off by one row skips or
> repeats at the page boundary and nothing else in the suite would notice — the pages would still
> be the right size and still be in the right order.

---

## Anti-pattern 3 — Loop-and-save

**Before shape:** read every matching order, then `for (order : orders) { order.setStatus(CANCELLED);
repo.save(order); }` — N hydrated entities and N writes.

**After** — the port names the business operation:

```java
// domain/repositories/OrderRepository.java
int cancelOutstandingForSku(Sku sku);
int deliverPlacedOlderThan(Instant cutoff);
```

The adapter answers it in one statement — `infrastructure/.../OrderJpaRepository.java`:

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE OrderJpaEntity o SET o.status = :cancelled "
     + "WHERE o.sku = :sku AND o.status = :placed")
int cancelOutstandingForSku(@Param("sku") String sku,
                            @Param("placed") OrderStatus placed,
                            @Param("cancelled") OrderStatus cancelled);
```

> One statement for the whole recall. The `status = :placed` guard mirrors `Order.cancel()`: only a
> placed order can be cancelled, so the returned count means "placed orders this recall cancelled",
> and re-running the recall is a no-op. `flushAutomatically` so a pending insert in the same
> transaction is visible to the update; `clearAutomatically` so no entity already in the persistence
> context keeps reporting the status it had before the update ran behind its back.

Those two flags are the JPA trap most people hit; they are not optional decoration.

The use case collapses to nothing — `usecases/order/RecallSku.java`:

```java
response.setCancelledCount(orderRepository.cancelOutstandingForSku(Sku.of(sku)));
```

---

## Anti-pattern 4 — Read-modify-write (and the "business logic in the database" argument)

This is the case that *sounds* like it violates the rule, and it is the one worth arguing out loud.

**Before** — `backend-java/.../core/services/CouponService.java`:

```java
public void incrementUsageCount(String couponCode) {
    var coupon = couponRepository.findByCode(couponCode).get();
    coupon.setUsedCount(coupon.getUsedCount() + 1);   // two concurrent callers both read 4,
    couponRepository.save(coupon);                    // both write 5. One redemption vanishes.
}
```

**After** — the port asks a business question:

```java
// domain/repositories/CouponRepository.java
boolean tryRedeem(CouponCode code);
```

```java
// infrastructure/.../CouponJpaRepository.java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("UPDATE CouponJpaEntity c SET c.usedCount = c.usedCount + 1 "
     + "WHERE c.code = :code AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
int redeemIfAvailable(@Param("code") String code);
```

```java
// infrastructure/.../CouponRepositoryAdapter.java
@Transactional
@Override
public boolean tryRedeem(CouponCode code) {
    return jpaRepository.redeemIfAvailable(code.value()) == 1;
}
```

From the query's own comment:

> the check and the increment as one statement, so nothing can happen between them.
> `used_count = used_count + 1` reads the column inside the write rather than trusting a value the
> application read earlier, and **the WHERE clause is the usage limit itself** — so a row count of
> zero is not a failure to find the coupon, it is the coupon saying no.

### The objection, answered

| | |
|---|---|
| **What gets said** | "Business logic does not belong in the database." |
| **What is actually true** | Business *decisions* belong in the domain. *Atomicity* belongs where the data is. The domain can express "a coupon at its limit may not be redeemed." It cannot express "…and nobody else may redeem it between my read and my write." |
| **What it costs** | The usage-limit rule now exists twice: `Coupon#usageLimitReached` and that `WHERE` clause. That is a real duplication, not a rhetorical one. |
| **How it stays honest** | `integrationTest/.../CouponRedemptionConcurrencyIntegrationTest` exercises **both** paths and demonstrates the lost update. The duplication is pinned by a test, not by a comment. |

The slow path is kept deliberately — from `CouponRepository`:

> Last-writer-wins on the count it is given, which is exactly why it is not how a redemption is
> recorded: `tryRedeem` is. Kept because the read-modify-write path is worth being able to
> demonstrate losing an update.

---

## Anti-pattern 5 — Hydrating the domain just to serialize it

Every read in the CRUD version builds `Order` objects, runs constructors, populates the persistence
context and gets dirty-checked — in order to copy fields into a response DTO.

`JpaOrderQuery`:

```java
private static final String LIST_SELECT = """
        SELECT o.order_number, o.order_timestamp, o.sku, o.country, o.quantity,
               o.total_price, o.status, o.applied_coupon_code
        FROM orders o
        """;
```

> The SELECT list is the projection: these queries fetch the columns the response holds and nothing
> else, and no `OrderJpaEntity` is ever materialised — so nothing enters the persistence context,
> nothing is dirty-checked, and no domain constructor runs.

`src/benchmark/.../Theme2Baseline.java` quantifies what the hydrating path costs:

```java
private static final long NO_DOMAIN_OBJECTS = 0;
private static final long ORDER_OBJECTS_PER_ROW = 11;    // what a hydrating read costs, per row
private static final long COUPON_OBJECTS_PER_ROW = 5;
```

### The correctness argument, which is stronger than the speed one

From `ArchitectureTest`:

```java
@ArchTest
static final ArchRule READ_USECASES_DO_NOT_TOUCH_THE_DOMAIN = noClasses()
        .that().resideInAPackage("..usecases.queries..")
        .or().haveFullyQualifiedName(BrowseCoupons.class.getName())
        .or().haveFullyQualifiedName(BrowseOrderHistory.class.getName())
        .or().haveFullyQualifiedName(ViewOrderDetails.class.getName())
        .or().haveFullyQualifiedName(ViewSalesReport.class.getName())
        .should().dependOnClassesThat().resideInAPackage("..domain..")
        .because("a pure query reports what is stored; it does not re-run the rules that wrote it");
```

> Before this rule, `BrowseCoupons` imported `Coupon` and `CouponRepository` and **one row with a
> zero discount rate failed the whole list.**

Hydrating the write model on a read path does not only cost time — it makes *displaying* data fail
on *write-side* invariants. A row already in the database, that you are only trying to show, takes
down the whole page.

### The trade-off, stated

From `usecases/queries/package-info.java`:

> What is given up: the read model can now drift from the domain's idea of an order or a coupon,
> because nothing forces the two to agree. Speed and failure-isolation are bought by surrendering
> the guarantee that what is displayed was validated by the rules that wrote it.

---

## Anti-pattern 6 — `save()` costing a SELECT

Covered in [Decoupling the Domain from the ORM](decoupling-domain-from-orm.md#4a-the-port-plain-java-named-after-intent),
and it belongs here as the cheapest win on the list:

```java
void add(Order order);      // PlaceOrder just minted a UUID-based number - it cannot exist
void update(Order order);   // CancelOrder is holding a row it just read
```

```java
// mapped entity carries no id, so Spring Data sees a new instance: one INSERT, no SELECT
public void add(Order order) { jpaRepository.save(OrderMapper.toEntity(order)); }
```

The domain always knew which one it was. The ORM's vocabulary made the code forget.

---

## Using the database well: `JpaSalesReportQuery`

Three statements, no entities, no loops — the aggregation happens where the rows already are:

```sql
SELECT o.country,
       date_trunc('month', o.order_timestamp) AS month,
       COUNT(*)              AS order_count,
       SUM(o.quantity)       AS quantity,
       SUM(o.subtotal_price) AS subtotal_price,
       SUM(o.tax_amount)     AS tax_amount,
       SUM(o.total_price)    AS total_price
FROM orders o
WHERE o.status <> :status
GROUP BY o.country, month
ORDER BY month DESC, o.country
```

The `LIMIT` on the top-SKU query is the point of that query:

> the database stops after n rows, instead of the caller receiving every SKU and discarding the
> tail.

And the join subtlety worth a slide of its own:

```sql
FROM coupons c
LEFT JOIN orders o
       ON o.applied_coupon_code = c.code
      AND o.status <> :status     -- in the JOIN, not the WHERE
GROUP BY c.code, c.usage_limit, c.used_count
```

> The status predicate sits in the JOIN condition, not in WHERE: in WHERE it would discard the
> coupon whose only orders were cancelled, turning the left join back into an inner one.

"Use the database properly" is a skill, not a shortcut.

---

## Where each concern belongs

| Concern | Where it lives | Why |
|---|---|---|
| "May this order be cancelled?" | Domain — `Order.cancel()` | A decision about a thing's lifecycle |
| "Is the coupon inside its validity window?" | Domain — `Coupon.discountAt()` | Same |
| "How is the total computed?" | Domain — `OrderPricing.price()` | Typed arithmetic, testable with no database |
| "Are orders blocked right now?" | Domain policy — `YearEndBlackoutPolicy` | A rule about time, not about a row |
| "How big may a page be?" | Use case — `PageSpec.MAX_SIZE` | Protects the system from a caller |
| **Filtering, sorting, limiting** | **Database** | Set operations over rows it already holds, with indexes |
| **Aggregation (`SUM`, `GROUP BY`)** | **Database** | Moving rows in order to total them is the waste |
| **Atomic check-and-set** | **Database** | The only place the gap between check and set can be closed |
| **Dialect, index choice, query plan** | **Adapter only** | `date_trunc` in an adapter is fine; in a use case it is a leak |

The database is not a dumb bucket to be decoupled *from*. It is a set-processing engine to be
decoupled *to a single layer*, so it can be used hard without infecting the core.
