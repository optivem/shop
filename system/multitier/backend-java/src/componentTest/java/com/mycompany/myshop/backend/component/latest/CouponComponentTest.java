package com.mycompany.myshop.backend.component.latest;

import com.mycompany.myshop.backend.AbstractComponentTest;
import org.junit.jupiter.api.Test;

/**
 * "After" of the component-test refactor: the publish + browse coupon flow written on the scenario
 * DSL ({@link com.mycompany.myshop.backend.testkit.dsl.core.ScenarioDslImpl}) instead of raw {@code
 * restTemplate} calls. Same scenario as the {@code legacy/} twin.
 *
 * <p>Coupon touches no external systems, so — unlike the order twins, which additionally swap raw
 * WireMock for stub programming through the DSL — this pair's only axis is SUT-side. Publish answers
 * {@code 204 No Content} with no body, which {@code shouldSucceed()} asserts; {@code coupon("SAVE10")}
 * then reads the coupon back through {@code GET /api/coupons}, so reaching the step at all is the
 * "browse lists it" assertion.
 */
class CouponComponentTest extends AbstractComponentTest {

    @Test
    void publishReturnsNoContentThenBrowseListsCoupon() {
        scenario.when().publishCoupon()
                .withCouponCode("SAVE10").withDiscountRate("0.20").withUsageLimit(100)
            .then().shouldSucceed()
            .and().coupon("SAVE10")
                .hasDiscountRate("0.20")
                .hasUsageLimit(100)
                .hasUsedCount(0);
    }
}
