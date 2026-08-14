package com.mycompany.myshop.backend.contract.latest.internal.frontend;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.core.entities.Coupon;
import com.mycompany.myshop.backend.core.entities.Order;
import com.mycompany.myshop.backend.core.entities.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Replays the frontend consumer contract against the in-process provider, with external
 * systems WireMock-stubbed and provider states seeded into the Testcontainers Postgres. Fails the
 * build if the backend drifts from the contract. Every interaction in the pact is verified.
 *
 * <p>The contract is read from the repo-owned {@code shop/contracts/} folder (the consumer writes
 * the pact there; this provider reads it from the same neutral location).
 */
@Provider("backend")
@PactFolder("../../../contracts")
class BackendPactVerificationTest extends BaseComponentTest {

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
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
        app.erp().returnsProduct().sku("BOOK-123").unitPrice("10.00").execute();
        app.erp().returnsPromotion().active(false).discount("1.0").execute();
        app.tax().returnsTaxRate().country("US").taxRate("0.10").execute();
    }

    @State("order placement is blocked by the New Year blackout")
    void orderPlacementBlackout() {
        app.clock().returnsTime().time("2026-12-31T23:59:00Z").execute();
    }

    /**
     * Everything the order needs is in place — the coupon is the one thing missing, so the rejection
     * the frontend renders is the coupon's and not something else failing first. The base
     * {@code @BeforeEach} empties the coupon table, so nothing has to be un-seeded.
     */
    @State("coupon INVALIDCOUPON does not exist")
    void couponInvalidCouponDoesNotExist() {
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
        app.erp().returnsProduct().sku("BOOK-123").unitPrice("10.00").execute();
        app.erp().returnsPromotion().active(false).discount("1.0").execute();
        app.tax().returnsTaxRate().country("US").taxRate("0.10").execute();
    }

    /**
     * ERP is the one that says a SKU is not a product, so the ERP stub is what makes this state — the
     * clock still has to answer, because it is consulted before the product is looked up.
     */
    @State("product NON-EXISTENT-SKU-12345 does not exist")
    void productDoesNotExist() {
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
        app.erp().returnsNoProduct().sku("NON-EXISTENT-SKU-12345").execute();
    }

    /**
     * The product must resolve for the country to be the thing that fails — Tax is consulted only
     * after ERP has answered.
     */
    @State("product BOOK-123 exists and XX is not taxable")
    void countryXxNotTaxable() {
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
        app.erp().returnsProduct().sku("BOOK-123").unitPrice("10.00").execute();
        app.erp().returnsPromotion().active(false).discount("1.0").execute();
        app.tax().returnsNoTaxRate().country("XX").execute();
    }

    /** 22:15 on December 31st — inside the cancellation blackout, with a cancellable order to aim at. */
    @State("order cancellation is blocked by the New Year blackout")
    void orderCancellationBlackout() {
        app.clock().returnsTime().time("2026-12-31T22:15:00Z").execute();
        orderRepository.save(sampleOrder("ORD-1"));
    }

    @State("at least one order exists")
    void atLeastOneOrderExists() {
        orderRepository.save(sampleOrder("ORD-HIST-1"));
    }

    /**
     * Shared by viewing ORD-1 and by cancelling it. Cancelling asks the clock before it touches the
     * order (the blackout is decided first), so the clock is stubbed here too — at a date well clear
     * of the blackout, which is what makes the cancellation succeed.
     */
    @State("order ORD-1 is placed")
    void orderOrd1Placed() {
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
        orderRepository.save(sampleOrder("ORD-1"));
    }

    @State("order ORD-1 is cancelled")
    void orderOrd1Cancelled() {
        orderRepository.save(sampleOrder("ORD-1", OrderStatus.CANCELLED));
    }

    @State("order ORD-1 is delivered")
    void orderOrd1Delivered() {
        orderRepository.save(sampleOrder("ORD-1", OrderStatus.DELIVERED));
    }

    @State("no order UNKNOWN exists")
    void noOrderUnknownExists() {
        // DB is emptied in the base @BeforeEach, so no UNKNOWN order exists.
    }

    @State("at least one coupon exists")
    void atLeastOneCouponExists() {
        couponRepository.save(new Coupon("SAVE10", new BigDecimal("0.20"), null, null, 100, 0));
    }

    @State("coupon SAVE10 exists")
    void couponSave10Exists() {
        couponRepository.save(new Coupon("SAVE10", new BigDecimal("0.20"), null, null, 100, 0));
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
        return new Order(
            orderNumber, Instant.parse("2026-03-10T12:00:00Z"), "US",
            "BOOK-123", 2, new BigDecimal("10.00"), new BigDecimal("20.00"),
            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00"),
            new BigDecimal("0.10"), new BigDecimal("2.00"), new BigDecimal("22.00"),
            status, null);
    }
}
