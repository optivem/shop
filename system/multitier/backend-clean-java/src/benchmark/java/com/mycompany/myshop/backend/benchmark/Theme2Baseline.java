package com.mycompany.myshop.backend.benchmark;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.infrastructure.persistence.mappers.CouponMapper;
import com.mycompany.myshop.backend.infrastructure.persistence.mappers.OrderMapper;
import com.mycompany.myshop.backend.infrastructure.persistence.repositories.CouponJpaRepository;
import com.mycompany.myshop.backend.infrastructure.persistence.repositories.OrderJpaRepository;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("benchmark")
@Import(TestcontainersConfiguration.class)
class Theme2Baseline {

    // Chunk R took the read path off the domain model, so the three browse/view rows below now build
    // nothing at all. The two per-row counts are still what a hydrating path costs, and the rows that
    // still hydrate -- the in-memory aggregation and the loop-and-save recall, both of them the
    // before-picture Chunks B and C replace -- still pay it.
    private static final long NO_DOMAIN_OBJECTS = 0;

    private static final long ORDER_OBJECTS_PER_ROW = 11;

    private static final long COUPON_OBJECTS_PER_ROW = 5;

    private static final int SINGLE_ROW_REPETITIONS = 1000;

    private static final int REDEMPTIONS = 100;

    private static final String RECALLED_SKU = "SKU-007";

    private static final String SET_BASED_RECALLED_SKU = "SKU-008";

    private static final int FIRST_CONDITIONAL_COUPON = REDEMPTIONS + 1;

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

    // The before-picture needs findAll()-shaped reads, and after Chunk R the domain repositories no
    // longer offer them -- deliberately: a port that names a mechanism is what forced the loop into
    // the application layer in the first place. Reaching past the port to the JPA repository here is
    // how the harness keeps measuring the code the demo replaced.
    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private CouponJpaRepository couponJpaRepository;

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
        report.addFact("\"Domain objects\" counts what the operation constructs on the way to its answer. "
                + "After Chunk R the three read paths construct none: they project the columns the "
                + "response holds and never build an `Order` or a `Coupon`.");
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
        measureConditionalRedemption();
        measureBulkRecall();
        measureSetBasedRecall();

        var path = Path.of(REPORT);
        report.writeTo(path);
        System.out.println(System.lineSeparator() + report.render());
        System.out.println("Written to " + path.toAbsolutePath());
    }

    private void warmUp() {
        browseOrderHistory.execute(new BrowseOrderHistoryRequest(null));
        browseOrderHistory.execute(new BrowseOrderHistoryRequest(ORDER_FILTER));
        browseCoupons.execute(new BrowseCouponsRequest());
        viewOrderDetails.execute(new ViewOrderDetailsRequest(ORDER_UNDER_TEST));
        couponRepository.findByCode(CouponCode.of("DEMO-CPN-0001"));
    }

    private void measureBrowseOrderHistory() {
        var timed = probe.measure(() -> browseOrderHistory.execute(new BrowseOrderHistoryRequest(null)));
        var items = timed.value().value().getOrders();
        report.add(new BenchmarkReport.Row(CAP_FILTER,
                "`BrowseOrderHistory` with no filter",
                timed.millis(), items.size(), NO_DOMAIN_OBJECTS,
                timed.statements(), timed.retainedHeapMb()));
    }

    private void measureBrowseOrderHistoryFiltered() {
        var timed = probe.measure(() -> browseOrderHistory.execute(new BrowseOrderHistoryRequest(ORDER_FILTER)));
        var items = timed.value().value().getOrders();
        report.add(new BenchmarkReport.Row(CAP_FILTER,
                "`BrowseOrderHistory` filtered on `" + ORDER_FILTER + "`",
                timed.millis(), items.size(), NO_DOMAIN_OBJECTS,
                timed.statements(), timed.retainedHeapMb()));
    }

    private void measureBrowseCoupons() {
        var timed = probe.measure(() -> browseCoupons.execute(new BrowseCouponsRequest()));
        var items = timed.value().value().getCoupons();
        report.add(new BenchmarkReport.Row(CAP_FILTER,
                "`BrowseCoupons`",
                timed.millis(), items.size(), NO_DOMAIN_OBJECTS,
                timed.statements(), timed.retainedHeapMb()));
    }

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
                timed.millis(), SINGLE_ROW_REPETITIONS, NO_DOMAIN_OBJECTS,
                timed.statements(), timed.retainedHeapMb()));
    }

    private void measureInMemoryAggregation() {
        var timed = probe.measure(() -> {
            var orders = hydratedOrders();
            var coupons = hydratedCoupons();

            var revenueByCountryMonth = orders.stream()
                    .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                    .collect(Collectors.groupingBy(
                            order -> order.getCountry().value() + "/" + month(order),
                            Collectors.reducing(BigDecimal.ZERO,
                                    order -> order.getPricing().totalPrice().amount(), BigDecimal::add)));

            var revenueBySku = orders.stream()
                    .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                    .collect(Collectors.groupingBy(order -> order.getSku().value(),
                            Collectors.reducing(BigDecimal.ZERO,
                                    order -> order.getPricing().totalPrice().amount(), BigDecimal::add)));

            // The hand-rolled join the plan calls out: two findAll()s and a lookup per coupon.
            var discountByCoupon = orders.stream()
                    .filter(order -> order.getAppliedCouponCode() != null)
                    .filter(order -> order.getStatus() != OrderStatus.CANCELLED)
                    .collect(Collectors.groupingBy(
                            order -> order.getAppliedCouponCode().value(),
                            Collectors.reducing(BigDecimal.ZERO,
                                    order -> order.getPricing().discountAmount().amount(), BigDecimal::add)));
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

    private void measureConditionalRedemption() {
        var timed = probe.measure(() -> {
            var redeemed = 0L;
            for (var i = FIRST_CONDITIONAL_COUPON; i < FIRST_CONDITIONAL_COUPON + REDEMPTIONS; i++) {
                if (couponRepository.tryRedeem(CouponCode.of(String.format("DEMO-CPN-%04d", i)))) {
                    redeemed++;
                }
            }
            return redeemed;
        });
        report.addFact("Conditional redemption: " + timed.value() + " of " + REDEMPTIONS
                + " attempts were accepted; the rest were coupons already at their usage limit, "
                + "which the read-modify-write loop above incremented anyway.");
        report.add(new BenchmarkReport.Row(CAP_ATOMIC,
                "`tryRedeem`, " + REDEMPTIONS + " coupons",
                timed.millis(), timed.value(), 0,
                timed.statements(), timed.retainedHeapMb()));
    }

    private void measureBulkRecall() {
        var timed = probe.measure(() -> {
            var cancelled = 0L;
            for (var order : hydratedOrders()) {
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

    private void measureSetBasedRecall() {
        var timed = probe.measure(() ->
                (long) orderRepository.cancelOutstandingForSku(Sku.of(SET_BASED_RECALLED_SKU)));
        report.add(new BenchmarkReport.Row(CAP_SET_WRITE,
                "Recall `" + SET_BASED_RECALLED_SKU + "`: one `UPDATE … WHERE`",
                timed.millis(), timed.value(), 0,
                timed.statements(), timed.retainedHeapMb()));
    }

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
        report.addPlan("What the set-based recall issues instead (planned, not executed)",
                probe.explainOnly("UPDATE orders SET status = 'CANCELLED' "
                        + "WHERE sku = '" + RECALLED_SKU + "' AND status <> 'CANCELLED'"));
    }

    // What findAll() + map-to-domain cost, kept measurable now that no port offers it.
    private List<Order> hydratedOrders() {
        return orderJpaRepository.findAll().stream().map(OrderMapper::toDomain).toList();
    }

    private List<Coupon> hydratedCoupons() {
        return couponJpaRepository.findAll().stream().map(CouponMapper::toDomain).toList();
    }

    private static String month(Order order) {
        return DateTimeFormatter.ofPattern("yyyy-MM")
                .withZone(ZoneOffset.UTC)
                .format(order.getOrderTimestamp());
    }
}
