package com.mycompany.myshop.backend.benchmark;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCoupons;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCouponsRequest;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistory;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryRequest;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetails;
import com.mycompany.myshop.backend.usecases.order.ViewOrderDetailsRequest;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The theme-2 measurement harness: what the application does today, for each capability the plan
 * says the database is barred from.
 *
 * <p><strong>This is not a test.</strong> It asserts nothing and can fail only by throwing, which is
 * why it sits in its own source set rather than in {@code integrationTest}. Its output is a number,
 * not a verdict, and a number that moved is not a regression.
 *
 * <p>It runs the <em>real</em> use cases against a real Postgres, through the real ports and the
 * real adapters — not a hand-written imitation of them. That is the whole reason it exists: the
 * claim "the loop is in the application layer" is only worth measuring if the thing measured is the
 * loop the application actually runs.
 *
 * <p>Where a capability has no call site yet — a bulk recall, an aggregate report — the harness
 * writes the honest in-memory alternative here, using only what the ports offer today. That code is
 * the "before" picture, and it lives here rather than in {@code src/main} because nobody should ship
 * it.
 *
 * <p>Order matters: every non-mutating measurement is taken first, over pristine seed data, and the
 * two mutating ones run last. Re-running the task re-seeds from scratch, so a run is repeatable
 * regardless.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("benchmark")
@Import(TestcontainersConfiguration.class)
class Theme2Baseline {

    /**
     * Domain objects {@code OrderMapper.toDomain} constructs for one row: six {@code Money}
     * (unit, base, discount, subtotal, tax, total), two {@code Rate} (discount, tax), one
     * {@code OrderPricing}, one {@code Country} and the {@code Order} itself. A row that carries a
     * coupon constructs a {@code CouponCode} as well, counted separately because only 30% of the
     * seed does.
     */
    private static final long ORDER_OBJECTS_PER_ROW = 11;

    /**
     * Domain objects {@code CouponMapper.toDomain} constructs for one row: {@code CouponCode},
     * {@code Rate}, {@code ValidityPeriod}, {@code UsageQuota}, and the {@code Coupon}.
     */
    private static final long COUPON_OBJECTS_PER_ROW = 5;

    /** Enough repetitions that a single-row read is timed rather than rounded to zero. */
    private static final int SINGLE_ROW_REPETITIONS = 1000;

    /** How many coupons the read-modify-write measurement redeems. */
    private static final int REDEMPTIONS = 100;

    /** The SKU the bulk-recall measurement withdraws. The seed spreads 100k orders over 50 SKUs. */
    private static final String RECALLED_SKU = "SKU-007";

    private static final String ORDER_UNDER_TEST = "DEMO-ORD-050000";
    private static final String ORDER_FILTER = "DEMO-ORD-0500";
    private static final String SEED = "../../db/seed/demo-volume.sql";
    private static final String REPORT = "build/benchmark/theme2-baseline.md";

    private static final String CAP_FILTER = "Filtering, sorting, limiting";
    private static final String CAP_PROJECT = "Projecting only the columns asked for";
    private static final String CAP_AGGREGATE = "Aggregation and joins";
    private static final String CAP_ATOMIC = "Atomic read-modify-write";
    private static final String CAP_SET_WRITE = "Set-based writes";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private BrowseOrderHistory browseOrderHistory;

    @Autowired
    private BrowseCoupons browseCoupons;

    @Autowired
    private ViewOrderDetails viewOrderDetails;

    private Probe probe;
    private BenchmarkReport report;

    @BeforeEach
    void seed() {
        probe = new Probe(dataSource, entityManagerFactory);
        report = new BenchmarkReport();
        probe.runScript(Path.of(SEED));
        report.addFact("`" + SEED + "`, deterministic — no `random()` anywhere in it.");
        report.addFact(probe.countRows("orders") + " rows in `orders`, "
                + probe.countRows("coupons") + " rows in `coupons`.");
    }

    @Test
    void measureWhatTheApplicationDoesToday() {
        warmUp();
        measureBrowseOrderHistory();
        measureBrowseOrderHistoryFiltered();
        measureBrowseCoupons();
        measureViewOrderDetails();
        measureInMemoryAggregation();
        capturePlans();

        // Mutating from here down.
        measureCouponRedemption();
        measureBulkRecall();

        var path = Path.of(REPORT);
        report.writeTo(path);
        System.out.println(System.lineSeparator() + report.render());
        System.out.println("Written to " + path.toAbsolutePath());
    }

    /**
     * Runs every read path once, untimed, before anything is measured.
     *
     * <p>Without this the first measurement on each path pays for Hibernate's query plan cache, the
     * connection pool filling, and the JIT — the first run of {@code BrowseCoupons} took 866 ms for
     * 300 rows whose plan executes in 0.03 ms. That number is real but it is not the number the
     * demo is about, and quoting it would be quoting a warm-up as if it were a cost. The mutating
     * measurements are warmed by proxy: they run last, on paths these reads have already opened.
     */
    private void warmUp() {
        browseOrderHistory.execute(new BrowseOrderHistoryRequest(null));
        browseOrderHistory.execute(new BrowseOrderHistoryRequest(ORDER_FILTER));
        browseCoupons.execute(new BrowseCouponsRequest());
        viewOrderDetails.execute(new ViewOrderDetailsRequest(ORDER_UNDER_TEST));
        couponRepository.findByCode(CouponCode.of("DEMO-CPN-0001"));
    }

    /**
     * The unbounded list read. Every row in the table is hydrated into an {@code Order}, unwrapped
     * into a response item, and the {@code Order} is then unreachable.
     */
    private void measureBrowseOrderHistory() {
        var timed = probe.measure(() -> browseOrderHistory.execute(new BrowseOrderHistoryRequest(null)));
        var items = timed.value().value().getOrders();
        var withCoupon = items.stream().filter(item -> item.getAppliedCouponCode() != null).count();
        report.add(new BenchmarkReport.Row(CAP_FILTER,
                "`BrowseOrderHistory` with no filter",
                timed.millis(), items.size(),
                items.size() * ORDER_OBJECTS_PER_ROW + withCoupon,
                timed.statements(), timed.retainedHeapMb()));
    }

    /** The same read narrowed by a substring filter — pushed down to SQL, but still unbounded. */
    private void measureBrowseOrderHistoryFiltered() {
        var timed = probe.measure(() -> browseOrderHistory.execute(new BrowseOrderHistoryRequest(ORDER_FILTER)));
        var items = timed.value().value().getOrders();
        var withCoupon = items.stream().filter(item -> item.getAppliedCouponCode() != null).count();
        report.add(new BenchmarkReport.Row(CAP_FILTER,
                "`BrowseOrderHistory` filtered on `" + ORDER_FILTER + "`",
                timed.millis(), items.size(),
                items.size() * ORDER_OBJECTS_PER_ROW + withCoupon,
                timed.statements(), timed.retainedHeapMb()));
    }

    /** The other unbounded list read, over a table the demo keeps small on purpose. */
    private void measureBrowseCoupons() {
        var timed = probe.measure(() -> browseCoupons.execute(new BrowseCouponsRequest()));
        var items = timed.value().value().getCoupons();
        report.add(new BenchmarkReport.Row(CAP_FILTER,
                "`BrowseCoupons`",
                timed.millis(), items.size(), items.size() * COUPON_OBJECTS_PER_ROW,
                timed.statements(), timed.retainedHeapMb()));
    }

    /**
     * The sharpest projection case: fifteen response fields, every one a column, reached by building
     * a whole {@code Order} and immediately calling {@code .amount()} / {@code .value()} on it.
     */
    private void measureViewOrderDetails() {
        var timed = probe.measure(() -> {
            var request = new ViewOrderDetailsRequest(ORDER_UNDER_TEST);
            for (var i = 0; i < SINGLE_ROW_REPETITIONS - 1; i++) {
                viewOrderDetails.execute(request);
            }
            return viewOrderDetails.execute(request);
        });
        report.add(new BenchmarkReport.Row(CAP_PROJECT,
                "`ViewOrderDetails` × " + SINGLE_ROW_REPETITIONS,
                timed.millis(), SINGLE_ROW_REPETITIONS,
                SINGLE_ROW_REPETITIONS * (ORDER_OBJECTS_PER_ROW + 1),
                timed.statements(), timed.retainedHeapMb()));
    }

    /**
     * The report Chunk B will push into one statement, written the only way today's ports allow:
     * load both tables and group them in Java. Nothing in {@code src/main} does this yet — this is
     * the alternative the plan says a naive implementation would reach for, measured rather than
     * assumed.
     */
    private void measureInMemoryAggregation() {
        var timed = probe.measure(() -> {
            var orders = orderRepository.findAllByOrderByOrderTimestampDesc();
            var coupons = couponRepository.findAll();

            var revenueByCountryMonth = orders.stream()
                    .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                    .collect(Collectors.groupingBy(
                            order -> order.getCountry().value() + "/" + month(order),
                            Collectors.reducing(BigDecimal.ZERO,
                                    order -> order.getTotalPrice().amount(), BigDecimal::add)));

            var revenueBySku = orders.stream()
                    .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                    .collect(Collectors.groupingBy(Order::getSku,
                            Collectors.reducing(BigDecimal.ZERO,
                                    order -> order.getTotalPrice().amount(), BigDecimal::add)));

            // The hand-rolled join the plan calls out: two findAll()s and a lookup per coupon.
            var discountByCoupon = orders.stream()
                    .filter(order -> order.getAppliedCouponCode() != null)
                    .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                    .collect(Collectors.groupingBy(
                            order -> order.getAppliedCouponCode().value(),
                            Collectors.reducing(BigDecimal.ZERO,
                                    order -> order.getDiscountAmount().amount(), BigDecimal::add)));
            var effectiveness = coupons.stream()
                    .collect(Collectors.toMap(
                            coupon -> coupon.getCode().value(),
                            coupon -> discountByCoupon.getOrDefault(coupon.getCode().value(), BigDecimal.ZERO),
                            (first, second) -> first));

            return List.<Map<String, BigDecimal>>of(revenueByCountryMonth, revenueBySku, effectiveness);
        });

        var groups = timed.value().stream().mapToLong(Map::size).sum();
        report.add(new BenchmarkReport.Row(CAP_AGGREGATE,
                "Three reports, in Java, over two `findAll()`s",
                timed.millis(), groups,
                probe.countRows("orders") * ORDER_OBJECTS_PER_ROW
                        + probe.countRows("coupons") * COUPON_OBJECTS_PER_ROW,
                timed.statements(), timed.retainedHeapMb()));
    }

    /**
     * Coupon redemption as {@code PlaceOrder} does it: read the coupon, increment in memory, write
     * it back. The cost measured here is round trips. The correctness problem is separate and is
     * what Chunk A4's concurrency test exists to prove — two of these interleaved lose an increment.
     */
    private void measureCouponRedemption() {
        var timed = probe.measure(() -> {
            var redeemed = 0L;
            for (var i = 1; i <= REDEMPTIONS; i++) {
                var code = CouponCode.of(String.format("DEMO-CPN-%04d", i));
                var coupon = couponRepository.findByCode(code);
                if (coupon.isPresent()) {
                    coupon.get().redeem();
                    couponRepository.save(coupon.get());
                    redeemed++;
                }
            }
            return redeemed;
        });
        report.add(new BenchmarkReport.Row(CAP_ATOMIC,
                "Read-modify-write, " + REDEMPTIONS + " coupons",
                timed.millis(), timed.value(), timed.value() * COUPON_OBJECTS_PER_ROW,
                timed.statements(), timed.retainedHeapMb()));
    }

    /**
     * A bulk SKU recall, written with the vocabulary the port offers today. {@code OrderRepository}
     * can only say "give me all the rows", so the filter is a Java {@code filter} and the write is
     * one {@code save} per order. This is the shape Chunk A replaces with a single
     * {@code UPDATE … WHERE}, and the statement count in this row is the argument.
     */
    private void measureBulkRecall() {
        var timed = probe.measure(() -> {
            var cancelled = 0L;
            for (var order : orderRepository.findAllByOrderByOrderTimestampDesc()) {
                if (RECALLED_SKU.equals(order.getSku()) && order.getStatus() != OrderStatus.CANCELLED) {
                    order.cancel();
                    orderRepository.save(order);
                    cancelled++;
                }
            }
            return cancelled;
        });
        report.add(new BenchmarkReport.Row(CAP_SET_WRITE,
                "Recall `" + RECALLED_SKU + "`: `findAll()` + filter + one `save` per order",
                timed.millis(), timed.value(),
                probe.countRows("orders") * ORDER_OBJECTS_PER_ROW,
                timed.statements(), timed.retainedHeapMb()));
    }

    /** The plans behind the numbers. Reads are analyzed; the one write is planned but not run. */
    private void capturePlans() {
        report.addPlan("`BrowseOrderHistory`, unfiltered",
                probe.explainAnalyze("SELECT * FROM orders ORDER BY order_timestamp DESC"));
        report.addPlan("`BrowseOrderHistory`, filtered",
                probe.explainAnalyze("SELECT * FROM orders "
                        + "WHERE lower(order_number) LIKE lower('%" + ORDER_FILTER + "%') "
                        + "ORDER BY order_timestamp DESC"));
        report.addPlan("`BrowseCoupons`",
                probe.explainAnalyze("SELECT * FROM coupons"));
        report.addPlan("`ViewOrderDetails`",
                probe.explainAnalyze("SELECT * FROM orders WHERE order_number = '" + ORDER_UNDER_TEST + "'"));
        report.addPlan("Coupon lookup by code",
                probe.explainAnalyze("SELECT * FROM coupons WHERE code = 'DEMO-CPN-0001'"));
        report.addPlan("The rows a recall of `" + RECALLED_SKU + "` has to find",
                probe.explainAnalyze("SELECT * FROM orders WHERE sku = '" + RECALLED_SKU + "'"));
        report.addPlan("What Chunk A will issue instead (planned, not executed)",
                probe.explainOnly("UPDATE orders SET status = 'CANCELLED' "
                        + "WHERE sku = '" + RECALLED_SKU + "' AND status <> 'CANCELLED'"));
    }

    private static String month(Order order) {
        return DateTimeFormatter.ofPattern("yyyy-MM")
                .withZone(ZoneOffset.UTC)
                .format(order.getOrderTimestamp());
    }
}
