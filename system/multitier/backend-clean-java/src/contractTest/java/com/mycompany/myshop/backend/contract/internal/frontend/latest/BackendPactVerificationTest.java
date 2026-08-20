package com.mycompany.myshop.backend.contract.internal.frontend.latest;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.mycompany.myshop.backend.BaseComponentTest;
import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.entities.Order;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.repositories.OrderRepository;
import com.mycompany.myshop.backend.domain.values.Country;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Money;
import com.mycompany.myshop.backend.domain.values.OrderNumber;
import com.mycompany.myshop.backend.domain.values.OrderPricing;
import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.Sku;
import com.mycompany.myshop.backend.domain.values.UsageQuota;
import com.mycompany.myshop.backend.domain.values.ValidityPeriod;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;

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
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
        app.erp().returnsProduct().sku("BOOK-123").unitPrice("10.00").execute();
        app.erp().returnsPromotion().active(false).discount("1.0").execute();
        app.tax().returnsTaxRate().country("US").taxRate("0.10").execute();
    }

    @State("order placement is blocked by the New Year blackout")
    void orderPlacementBlackout() {
        app.clock().returnsTime().time("2026-12-31T23:59:00Z").execute();
    }

    @State("coupon INVALIDCOUPON does not exist")
    void couponInvalidCouponDoesNotExist() {
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
        app.erp().returnsProduct().sku("BOOK-123").unitPrice("10.00").execute();
        app.erp().returnsPromotion().active(false).discount("1.0").execute();
        app.tax().returnsTaxRate().country("US").taxRate("0.10").execute();
    }

    @State("product NON-EXISTENT-SKU-12345 does not exist")
    void productDoesNotExist() {
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
        app.erp().returnsNoProduct().sku("NON-EXISTENT-SKU-12345").execute();
    }

    @State("product BOOK-123 exists and XX is not taxable")
    void countryXxNotTaxable() {
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
        app.erp().returnsProduct().sku("BOOK-123").unitPrice("10.00").execute();
        app.erp().returnsPromotion().active(false).discount("1.0").execute();
        app.tax().returnsNoTaxRate().country("XX").execute();
    }

    @State("order cancellation is blocked by the New Year blackout")
    void orderCancellationBlackout() {
        app.clock().returnsTime().time("2026-12-31T22:15:00Z").execute();
        orders.save(sampleOrder("ORD-1"));
    }

    @State("at least one order exists")
    void atLeastOneOrderExists() {
        orders.save(sampleOrder("ORD-HIST-1"));
    }

    @State("order ORD-1 is placed")
    void orderOrd1Placed() {
        app.clock().returnsTime().time("2026-03-10T12:00:00Z").execute();
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
        coupons.save(sampleCoupon());
    }

    @State("coupon SAVE10 exists")
    void couponSave10Exists() {
        coupons.save(sampleCoupon());
    }

    @State("no coupon SAVE10 exists yet")
    void noCouponSave10Exists() {
        // No-op: the base @BeforeEach empties the DB, so no SAVE10 coupon exists. The handler
        // must exist because pact-jvm runs state setup for the (now-verified) publish-coupon
        // interaction before the test body.
    }

    private Coupon sampleCoupon() {
        return new Coupon(CouponCode.of("SAVE10"), Rate.of("0.20"), ValidityPeriod.ALWAYS, UsageQuota.of(100, 0));
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
            OrderNumber.of(orderNumber), Instant.parse("2026-03-10T12:00:00Z"), Country.of("US"),
            Sku.of("BOOK-123"), pricing, status, null);
    }
}
