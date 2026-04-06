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
    @Channel(ChannelType.API)
    @ArgumentsSource(EmptyArgumentsProvider.class)
    void shouldRejectCouponWithBlankCode(String code) {
        scenario
                .when().publishCoupon()
                    .withCode(code)
                    .withDiscountRate(0.10)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("code", "Coupon code must not be blank");
    }

    @TestTemplate
    @Channel(ChannelType.API)
    @ValueSource(doubles = {0.0, -0.1})
    void shouldRejectCouponWithNonPositiveDiscountRate(double discountRate) {
        scenario
                .when().publishCoupon()
                    .withCode("INVALID")
                    .withDiscountRate(discountRate)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("discountRate", "Discount rate must be greater than 0.00");
    }

    @TestTemplate
    @Channel(ChannelType.API)
    @ValueSource(doubles = {1.01, 2.0})
    void shouldRejectCouponWithDiscountRateAboveOne(double discountRate) {
        scenario
                .when().publishCoupon()
                    .withCode("INVALID")
                    .withDiscountRate(discountRate)
                .then().shouldFail()
                    .errorMessage("The request contains one or more validation errors")
                    .fieldErrorMessage("discountRate", "Discount rate must be at most 1.00");
    }
}
