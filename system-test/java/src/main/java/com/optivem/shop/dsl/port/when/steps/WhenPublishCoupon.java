package com.optivem.shop.dsl.port.when.steps;

import com.optivem.shop.dsl.port.when.steps.base.WhenStep;

public interface WhenPublishCoupon extends WhenStep {
    WhenPublishCoupon withCode(String code);
    WhenPublishCoupon withDiscountRate(double discountRate);
    WhenPublishCoupon withDiscountRate(String discountRate);
}
