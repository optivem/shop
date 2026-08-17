package com.mycompany.myshop.backend.contract.internal.frontend.legacy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.entities.OrderStatus;
import com.mycompany.myshop.backend.domain.pricing.OrderPricing;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.UsageQuota;
import com.mycompany.myshop.backend.domain.values.ValidityPeriod;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * "Before" of the provider-side contract-tests refactor: the same pact replayed against the same
 * provider, but with every {@code @State} handler hand-writing its WireMock stubs inline instead of
 * seeding through the stub DSLs. The {@code latest/} twin drives the identical states through
 * {@code app.clock()} / {@code app.erp()} / {@code app.tax()}.
 *
 * <p>The repetition is the point, and is deliberately not factored into helpers: the same four
 * stub lines are re-typed across the states that need them, which is the duplication the stub DSLs
 * exist to remove. Extracting private {@code stubClock}/{@code stubProduct} helpers here would
 * already be half the refactor, and would make this a dishonest "before".
 *
 * <p>The URLs and JSON bodies are byte-identical to what the stub drivers register, so the two twins
 * are behaviour-neutral with respect to each other — the difference is vocabulary, not effect. Note
 * that the raw JSON below is the <em>external systems'</em> wire shape, which this variant's refactor
 * left untouched: it lives in {@code infrastructure/external/**} and is mapped to domain types by
 * the gateway adapters, so these literals are identical to {@code backend-java}'s.
 *
 * <p>Unlike the {@code component/legacy/} twins, which are self-contained and can be a representative
 * subset, this one is driven by the pact file: pact-jvm replays every interaction and fails on a
 * missing state handler. So every state in the pact must be handled here too, and a new consumer
 * interaction will break this twin until its handler is added.
 */
@Provider("backend")
@PactFolder("../../../contracts")
class BackendPactVerificationTest extends BaseComponentTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private CouponRepository coupons;

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("product BOOK-123 exists and US is taxable")
    void productExistsAndUsTaxable() {
        CLOCK.stubFor(get(urlEqualTo("/api/time"))
            .willReturn(okJson("{\"time\":\"2026-03-10T12:00:00Z\"}")));
        ERP.stubFor(get(urlEqualTo("/api/products/BOOK-123"))
            .willReturn(okJson("{\"id\":\"BOOK-123\",\"price\":10.00}")));
        ERP.stubFor(get(urlEqualTo("/api/promotion"))
            .willReturn(okJson("{\"promotionActive\":false,\"discount\":1.0}")));
        TAX.stubFor(get(urlEqualTo("/api/countries/US"))
            .willReturn(okJson("{\"id\":\"US\",\"countryName\":\"US\",\"taxRate\":0.10}")));
    }

    @State("order placement is blocked by the New Year blackout")
    void orderPlacementBlackout() {
        CLOCK.stubFor(get(urlEqualTo("/api/time"))
            .willReturn(okJson("{\"time\":\"2026-12-31T23:59:00Z\"}")));
    }

    /**
     * Everything the order needs is in place — the coupon is the one thing missing, so the rejection
     * the frontend renders is the coupon's and not something else failing first. The base
     * {@code @BeforeEach} empties the coupon table, so nothing has to be un-seeded.
     */
    @State("coupon INVALIDCOUPON does not exist")
    void couponInvalidCouponDoesNotExist() {
        CLOCK.stubFor(get(urlEqualTo("/api/time"))
            .willReturn(okJson("{\"time\":\"2026-03-10T12:00:00Z\"}")));
        ERP.stubFor(get(urlEqualTo("/api/products/BOOK-123"))
            .willReturn(okJson("{\"id\":\"BOOK-123\",\"price\":10.00}")));
        ERP.stubFor(get(urlEqualTo("/api/promotion"))
            .willReturn(okJson("{\"promotionActive\":false,\"discount\":1.0}")));
        TAX.stubFor(get(urlEqualTo("/api/countries/US"))
            .willReturn(okJson("{\"id\":\"US\",\"countryName\":\"US\",\"taxRate\":0.10}")));
    }

    /**
     * ERP is the one that says a SKU is not a product, so the ERP stub is what makes this state — the
     * clock still has to answer, because it is consulted before the product is looked up.
     */
    @State("product NON-EXISTENT-SKU-12345 does not exist")
    void productDoesNotExist() {
        CLOCK.stubFor(get(urlEqualTo("/api/time"))
            .willReturn(okJson("{\"time\":\"2026-03-10T12:00:00Z\"}")));
        ERP.stubFor(get(urlEqualTo("/api/products/NON-EXISTENT-SKU-12345"))
            .willReturn(aResponse().withStatus(404)));
    }

    /**
     * The product must resolve for the country to be the thing that fails — Tax is consulted only
     * after ERP has answered.
     */
    @State("product BOOK-123 exists and XX is not taxable")
    void countryXxNotTaxable() {
        CLOCK.stubFor(get(urlEqualTo("/api/time"))
            .willReturn(okJson("{\"time\":\"2026-03-10T12:00:00Z\"}")));
        ERP.stubFor(get(urlEqualTo("/api/products/BOOK-123"))
            .willReturn(okJson("{\"id\":\"BOOK-123\",\"price\":10.00}")));
        ERP.stubFor(get(urlEqualTo("/api/promotion"))
            .willReturn(okJson("{\"promotionActive\":false,\"discount\":1.0}")));
        TAX.stubFor(get(urlEqualTo("/api/countries/XX"))
            .willReturn(aResponse().withStatus(404)));
    }

    /** 22:15 on December 31st — inside the cancellation blackout, with a cancellable order to aim at. */
    @State("order cancellation is blocked by the New Year blackout")
    void orderCancellationBlackout() {
        CLOCK.stubFor(get(urlEqualTo("/api/time"))
            .willReturn(okJson("{\"time\":\"2026-12-31T22:15:00Z\"}")));
        orders.save(sampleOrder("ORD-1"));
    }

    @State("at least one order exists")
    void atLeastOneOrderExists() {
        orders.save(sampleOrder("ORD-HIST-1"));
    }

    /**
     * Shared by viewing ORD-1 and by cancelling it. Cancelling asks the clock before it touches the
     * order (the blackout is decided first), so the clock is stubbed here too — at a date well clear
     * of the blackout, which is what makes the cancellation succeed.
     */
    @State("order ORD-1 is placed")
    void orderOrd1Placed() {
        CLOCK.stubFor(get(urlEqualTo("/api/time"))
            .willReturn(okJson("{\"time\":\"2026-03-10T12:00:00Z\"}")));
        orders.save(sampleOrder("ORD-1"));
    }

    @State("order ORD-1 is cancelled")
    void orderOrd1Cancelled() {
        orders.save(sampleOrder("ORD-1", OrderStatus.CANCELLED));
    }

    @State("order ORD-1 is delivered")
    void orderOrd1Delivered() {
        orders.save(sampleOrder("ORD-1", OrderStatus.DELIVERED));
    }

    @State("no order UNKNOWN exists")
    void noOrderUnknownExists() {
        // DB is emptied in the base @BeforeEach, so no UNKNOWN order exists.
    }

    @State("at least one coupon exists")
    void atLeastOneCouponExists() {
        coupons.save(new Coupon(CouponCode.of("SAVE10"), Rate.of("0.20"), ValidityPeriod.ALWAYS, UsageQuota.of(100, 0)));
    }

    @State("coupon SAVE10 exists")
    void couponSave10Exists() {
        coupons.save(new Coupon(CouponCode.of("SAVE10"), Rate.of("0.20"), ValidityPeriod.ALWAYS, UsageQuota.of(100, 0)));
    }

    @State("no coupon SAVE10 exists yet")
    void noCouponSave10Exists() {
        // No-op: the base @BeforeEach empties the DB, so no SAVE10 coupon exists. The handler
        // must exist because pact-jvm runs state setup for the (now-verified) publish-coupon
        // interaction before the test body.
    }

    private Order sampleOrder(String orderNumber) {
        return sampleOrder(orderNumber, OrderStatus.PLACED);
    }

    private Order sampleOrder(String orderNumber, OrderStatus status) {
        var pricing = new OrderPricing(
            Money.of("10.00"), 2, Money.of("20.00"),
            Rate.ZERO, Money.ZERO, Money.of("20.00"),
            Rate.of("0.10"), Money.of("2.00"), Money.of("22.00"));

        return new Order(
            orderNumber, Instant.parse("2026-03-10T12:00:00Z"), "US",
            "BOOK-123", pricing, status, null);
    }
}
