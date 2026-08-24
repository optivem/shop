# Decoupling the Domain from the ORM

What actually belongs in the domain — instead of the ORM *being* the domain.

This repo carries the before-picture and the after-picture as two working implementations of the
same backend:

| | Path |
|---|---|
| **Before** — ORM as the domain | [`system/multitier/backend-java`](../../backend-java) |
| **After** — domain decoupled | [`system/multitier/backend-clean-java`](..) — this project |

Same HTTP contract, same database schema, same 62 component tests — and the component suite was
never edited to accommodate any of the rearrangement. Only `src/main` differs.

Related: [Designing for Performance](designing-for-performance.md) ·
[Decoupling from External Systems](decoupling-from-external-systems.md)

---

## 1. The symptom: the ORM *is* the domain

`backend-java/.../core/entities/Order.java` is named as domain, lives in `core`, and is 100%
persistence:

```java
package com.mycompany.myshop.backend.core.entities;

@Entity
@Table(name = "orders")
@Data                      // <- public setters on every field
@NoArgsConstructor
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                       // <- a DB concern in the model

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;            // <- String, not a type
    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;          // <- BigDecimal, not Money
    @Column(name = "discount_rate", precision = 5, scale = 4)
    private BigDecimal discountRate;
    // ...

    @Enumerated(EnumType.STRING)
    private OrderStatus status;            // <- settable by anyone
}
```

Three consequences follow, and they are the whole argument.

### a) The rules leak into a service

`core/services/OrderService.java` owns the state machine that `Order` should own:

```java
if (order.getStatus() != OrderStatus.PLACED) {
    throw new ValidationException("Order cannot be delivered in its current status");
}

order.setStatus(OrderStatus.DELIVERED);   // no method ever asked "may I?"
orderRepository.save(order);
```

### b) The arithmetic leaks too

Nine lines of untyped `BigDecimal` inside `placeOrder`:

```java
var basePrice      = unitPrice.multiply(BigDecimal.valueOf(quantity));
var promotedPrice  = basePrice.multiply(promotionFactor);
var discountAmount = promotedPrice.multiply(discountRate);
var subtotalPrice  = promotedPrice.subtract(discountAmount);
var taxAmount      = subtotalPrice.multiply(taxRate);
var totalPrice     = subtotalPrice.add(taxAmount);
```

Nothing stops you subtracting a tax *rate* from a *price*. They are the same type.

### c) The port *is* the ORM

`core/repositories/OrderRepository.java`:

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findAllByOrderByOrderTimestampDesc();
}
```

This interface is called a *repository* but it is a Spring Data extension point. Extending
`JpaRepository` does not give you `findByOrderNumber` — it gives you `save`, `saveAll`, `flush`,
`deleteAllInBatch`, `findAll(Pageable)`, roughly forty methods. The domain now has an API surface
nobody designed and nobody can defend.

---

## 2. What actually belongs in the domain

Four kinds of thing, and nothing else.

| Kind | In this repo | The test |
|---|---|---|
| **Entities** — identity, lifecycle, state transitions | `Order`, `Coupon` | Does it change over time, and do you care *which one* it is? |
| **Value objects** — meaning, no identity | `Money`, `Rate`, `OrderNumber`, `Sku`, `Country`, `OrderPricing` | Two with the same contents *are* the same thing |
| **Ports** — interfaces the inside owns | `OrderRepository`, `CouponRepository`, `ErpGateway`, and `GatewayException` beside them | Declared by the caller, implemented outside |
| **Policies** — rules belonging to no single entity | `YearEndBlackoutPolicy` | A rule about *when*, not about *a thing* |

And one line that decides most arguments, from the `backend-clean-java` README:

> **Only what MyShop owns is an entity.** `Product`, `TaxRate` and `Promotion` are snapshots of
> records the *ERP* and the tax service own — so they are values, in `domain/values`, not entities.

See [Decoupling from External Systems](decoupling-from-external-systems.md) for why that
classification matters operationally.

---

## 3. Decoupling ORM entities — three files, one job each

### 3a. The domain entity: a POJO with behaviour

`backend-clean-java/.../domain/entities/Order.java`:

```java
public class Order {

    private final OrderNumber orderNumber;   // typed, not String
    private final OrderPricing pricing;      // one value, not nine BigDecimals
    private OrderStatus status;              // the only mutable field
    // ...no id. Identity here is the order number.

    public static Order place(OrderNumber orderNumber, Instant orderTimestamp, Country country,
                              Sku sku, OrderPricing pricing, CouponCode appliedCouponCode) {
        return new Order(..., OrderStatus.PLACED, ...);   // <- starting status is a RULE
    }

    public static Order restore(OrderNumber orderNumber, ..., OrderStatus status, ...) {
        return new Order(..., status, ...);               // <- only the mapper calls this
    }

    private Order(...) { Guard.notNull(...); }            // <- constructor is private

    public void deliver() {
        if (status != OrderStatus.PLACED) {
            throw new ValidationException("Order cannot be delivered in its current status");
        }
        status = OrderStatus.DELIVERED;
    }

    public void cancel() {
        if (status == OrderStatus.CANCELLED) {
            throw new ValidationException("Order has already been cancelled");
        }
        if (status != OrderStatus.PLACED) {
            throw new ValidationException("Order cannot be cancelled in its current status");
        }
        status = OrderStatus.CANCELLED;
    }
}
```

Two things worth stopping on:

- **No `id`.** The surrogate key stops at persistence; the adapter resolves it on the way to the
  table. The domain's identity is `OrderNumber`.
- **`place()` vs `restore()`, with a private constructor.** This is the move JPA cannot make — JPA
  *requires* a public no-arg constructor and field access. Splitting creation from rehydration is
  what turns "a new order starts PLACED" from an argument the caller passes into a rule the type
  enforces.

### 3b. Value objects: where the arithmetic goes

`domain/values/Money.java` — illegal states cannot be constructed:

```java
public final class Money {
    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        if (amount.signum() < 0) throw new IllegalArgumentException("amount must not be negative");
        this.amount = amount;                     // <- no negative Money can exist. Anywhere.
    }

    public Money times(int quantity)   { return new Money(amount.multiply(BigDecimal.valueOf(quantity))); }
    public Money applyRate(Rate rate)  { return new Money(amount.multiply(rate.value())); }
    public Money plus(Money other)     { return new Money(amount.add(other.amount)); }
    public Money minus(Money other)    { return new Money(amount.subtract(other.amount)); }

    @Override public boolean equals(Object other) {
        return other instanceof Money m && amount.compareTo(m.amount) == 0;  // <- scale-insensitive
    }
}
```

> `Money` and `Rate` are hand-written classes rather than `record`s **because** `BigDecimal.equals`
> is scale-sensitive — `2.50` is not equal to `2.5`. A record's generated `equals` would be wrong.
> Everything else in `domain/values` is a record.

The nine loose `BigDecimal` lines become one named domain operation —
`domain/values/OrderPricing.java`:

```java
public static OrderPricing price(Money unitPrice, int quantity, Rate promotionFactor,
                                 Rate discountRate, Rate taxRate) {
    var basePrice      = unitPrice.times(quantity);
    var promotedPrice  = basePrice.applyRate(promotionFactor);
    var discountAmount = promotedPrice.applyRate(discountRate);
    var subtotalPrice  = promotedPrice.minus(discountAmount);
    var taxAmount      = subtotalPrice.applyRate(taxRate);
    var totalPrice     = subtotalPrice.plus(taxAmount);

    return new OrderPricing(unitPrice, quantity, basePrice, discountRate, discountAmount,
            subtotalPrice, taxRate, taxAmount, totalPrice);
}
```

Same six operations. Now `subtotalPrice.applyRate(taxRate)` is the only shape that compiles — you
cannot pass a `Money` where a `Rate` belongs.

### 3c. The JPA entity: dumb, wide, and in `infrastructure`

`infrastructure/persistence/entities/OrderJpaEntity.java` keeps **everything** deleted from the
domain — `id`, `@Column`, `@Data`, `precision`/`scale`, `@ColumnDefault`:

```java
@Entity @Table(name = "orders") @Data @NoArgsConstructor
public class OrderJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_number", nullable = false, unique = true) private String orderNumber;
    @Column(name = "unit_price", precision = 10, scale = 2) private BigDecimal unitPrice;
    // ...
}
```

This is the reassuring part: **nothing is given up.** Hibernate, Lombok, `@Data`, generated ids —
all still here, in the layer whose job they are. The schema did not change and the migrations did
not change. What changed is *who is allowed to see this class*.

### 3d. The mapper: the seam

`infrastructure/persistence/mappers/OrderMapper.java`:

```java
public static Order toDomain(OrderJpaEntity entity) {
    var pricing = new OrderPricing(
            Money.of(entity.getUnitPrice()), entity.getQuantity(),
            Money.of(entity.getBasePrice()),  Rate.of(entity.getDiscountRate()), ...);

    return Order.restore(                                   // <- restore, not place
            OrderNumber.of(entity.getOrderNumber()),
            entity.getOrderTimestamp(),
            Country.of(entity.getCountry()),
            Sku.of(entity.getSku()),
            pricing,
            entity.getStatus(),
            CouponCode.requested(entity.getAppliedCouponCode()).orElse(null));
}

public static OrderJpaEntity toEntity(Order order) {
    var entity = new OrderJpaEntity();
    entity.setOrderNumber(order.getOrderNumber().value());
    entity.setUnitPrice(order.getPricing().unitPrice().amount());
    // ...                                                  <- note: never sets id
    return entity;
}
```

The standing objection is "that is boilerplate." It is about forty lines, it is the only place
primitives get widened into types, and it is the only file that changes when the schema changes.
It buys a domain model with no framework in it.

---

## 4. Decoupling ORM repositories — port inside, adapter outside

### 4a. The port: plain Java, named after intent

`domain/repositories/OrderRepository.java` — no `@Repository`, no `JpaRepository`, no `Long`:

```java
public interface OrderRepository {
    void add(Order order);       // not save()
    void update(Order order);    // not save()

    Optional<Order> findByOrderNumber(OrderNumber orderNumber);

    int cancelOutstandingForSku(Sku sku);
    int deliverPlacedOlderThan(Instant cutoff);
}
```

**`add`/`update` instead of `save`**, from the port's own comment:

> no caller has ever been unsure which it meant: `PlaceOrder` has just minted an order number that
> cannot already exist, and `CancelOrder` and `DeliverOrder` are holding a row they just read. A
> single `save` threw that knowledge away and paid the database to work it out again — one wasted
> SELECT per write, on the hot path.

That is the strongest available argument, because it is not aesthetic: **decoupling from the ORM
made it faster.** `save()` is Hibernate's vocabulary, not the business's, and adopting it cost a
query.

**`cancelOutstandingForSku` / `deliverPlacedOlderThan`** name business operations that the CRUD
version expressed as a loop-and-save. See
[Designing for Performance](designing-for-performance.md#anti-pattern-3--loop-and-save).

### 4b. The adapter: where Spring Data actually lives

`infrastructure/persistence/adapters/OrderRepositoryAdapter.java`:

```java
@Component
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;   // <- THIS is what extends JpaRepository

    @Override
    public void add(Order order) {
        jpaRepository.save(OrderMapper.toEntity(order));  // no id on the mapped entity: INSERT, no SELECT
    }

    @Transactional
    @Override
    public void update(Order order) {
        jpaRepository.updateStatus(order.getOrderNumber().value(), order.getStatus());
    }

    @Override
    public Optional<Order> findByOrderNumber(OrderNumber orderNumber) {
        return jpaRepository.findByOrderNumber(orderNumber.value()).map(OrderMapper::toDomain);
    }
}
```

### 4c. What the seam buys: expressing what the domain cannot

`CouponRepositoryAdapter.tryRedeem` — a redemption that cannot lose an update:

```java
@Transactional
@Override
public boolean tryRedeem(CouponCode code) {
    return jpaRepository.redeemIfAvailable(code.value()) == 1;   // one atomic conditional UPDATE
}
```

The port asks a business question — *can this coupon be redeemed, and if so redeem it* — and the
adapter answers it in one statement. The read-modify-write version loses concurrent redemptions;
`CouponRedemptionConcurrencyIntegrationTest` demonstrates it. Full treatment in
[Designing for Performance](designing-for-performance.md#anti-pattern-4--read-modify-write-and-the-business-logic-in-the-database-argument).

---

## 5. The read side does not go through the domain at all

Not every read should build the write model. `usecases/queries/OrderQuery.java`:

```java
public interface OrderQuery {
    Page<OrderListItem> listOrders(String orderNumberFilter, PageSpec<OrderCursor> page);
    Optional<OrderDetail> findOrderDetail(String orderNumber);
}
```

It sits in `usecases/queries`, deliberately **not** `domain/repositories` —
`usecases/queries/package-info.java`:

> A port in the domain claims the domain needs it, and the domain never calls any of these.
> Placement is not what bypasses the domain model — the projection return type is; placement is
> what makes the code admit it.

Detail in
[Designing for Performance](designing-for-performance.md#anti-pattern-5--hydrating-the-domain-just-to-serialize-it).

---

## 6. Make it a build failure, not a code review

`src/test/.../ArchitectureTest.java`:

```java
@ArchTest
static final ArchRule DOMAIN_IS_FRAMEWORK_FREE = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..", "jakarta..", "com.fasterxml.jackson..",
                "lombok..", "org.hibernate..")
        .because("the domain is the centre: dependencies point inward, and it depends on nothing");

@ArchTest
static final ArchRule PERSISTENCE_IS_CONFINED_TO_INFRASTRUCTURE = noClasses()
        .that().resideOutsideOfPackage("..infrastructure..")
        .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..")
        .because("JPA entities and their mapping live in infrastructure.persistence only");

@ArchTest
static final ArchRule SPRING_DATA_IS_CONFINED_TO_INFRASTRUCTURE = noClasses()
        .that().resideOutsideOfPackage("..infrastructure..")
        .should().dependOnClassesThat().resideInAPackage("org.springframework.data..")
        .because("repository interfaces in the domain are plain Java; Spring Data implements them from outside");
```

The decoupling is not a convention people remember. It is a red build.

---

## What it costs

Stated plainly, because the trade is real:

- **A mapper per aggregate.** Roughly forty lines each, kept in step with the schema.
- **Two classes where there was one.** `Order` and `OrderJpaEntity` describe overlapping shapes.
- **`update()` is narrow on purpose.** `OrderJpaRepository.updateStatus` works precisely because
  `status` is the only non-final field on `Order`. If `Order` gains a second mutable field, that
  statement has to grow with it — the comment on it says so.
- **Round-trip mapping is new code with no other test.** Hence `OrderRepositoryIntegrationTest`
  and `CouponRepositoryIntegrationTest`, which pin domain → JPA → Postgres → domain.
  `backend-java` needed no equivalent because it had no mapping to get wrong.

---

## Suggested walkthrough order

1. `OrderService.placeOrder` — the nine `BigDecimal` lines and `order.setStatus(...)`. Ask where
   the domain is.
2. `OrderPricing.price` and `Order.cancel()` — the same behaviour, now owned by types.
3. Reveal `OrderJpaEntity`, unchanged, one package over. Nothing was given up.
4. `git log` the component tests: they never moved.
