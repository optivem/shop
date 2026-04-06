package com.optivem.shop.systemtest.latest.acceptance;

import com.optivem.shop.systemtest.commons.providers.EmptyArgumentsProvider;
import com.optivem.shop.systemtest.latest.acceptance.base.BaseAcceptanceTest;
import com.optivem.shop.dsl.channel.ChannelType;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

class PublishCouponNegativeTest extends BaseAcceptanceTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ValueSource(strings = {"0.0", "-0.01", "-0.15"})
    void cannotPublishCouponWithZeroOrNegativeDiscount(String discountRate) {
        scenario
                .when().publishCoupon()
                    .withCouponCode("INVALID-COUPON")
                    .withDiscountRate(discountRate)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("discountRate", "Discount rate must be greater than 0.00");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ValueSource(strings = {"1.01", "2.00"})
    void cannotPublishCouponWithDiscountGreaterThan100percent(String discountRate) {
        scenario
                .when().publishCoupon()
                    .withCouponCode("INVALID-COUPON")
                    .withDiscountRate(discountRate)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("discountRate", "Discount rate must be at most 1.00");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void cannotPublishCouponWithDuplicateCouponCode() {
        scenario
                .given().coupon()
                    .withCouponCode("EXISTING-COUPON")
                    .withDiscountRate(0.10)
                .when().publishCoupon()
                    .withCouponCode("EXISTING-COUPON")
                    .withDiscountRate(0.20)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("couponCode", "Coupon code EXISTING-COUPON already exists");
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @ValueSource(strings = {"0", "-1", "-100"})
    void cannotPublishCouponWithZeroOrNegativeUsageLimit(String usageLimit) {
        scenario
                .when().publishCoupon()
                    .withCouponCode("INVALID-LIMIT")
                    .withDiscountRate(0.15)
                    .withUsageLimit(usageLimit)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("usageLimit", "Usage limit must be positive");
    }

    @TestTemplate
    @Channel(ChannelType.API)
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectCouponWithBlankCode(String code) {
        scenario
                .when().publishCoupon()
                    .withCouponCode(code)
                    .withDiscountRate(0.10)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("code", "Coupon code must not be blank");
    }
}
