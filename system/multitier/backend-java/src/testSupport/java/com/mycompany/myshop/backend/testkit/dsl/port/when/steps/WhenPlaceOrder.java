package com.mycompany.myshop.backend.support.port.when.steps;

import com.mycompany.myshop.backend.support.port.when.steps.base.WhenStep;

public interface WhenPlaceOrder extends WhenStep {
    WhenPlaceOrder withSku(String sku);

    WhenPlaceOrder withQuantity(int quantity);

    /**
     * A quantity that is not an integer — {@code "3.5"}, {@code "lala"}, {@code ""}, or {@code null}.
     * {@code withQuantity(null)} binds here (the {@code int} overload cannot take null), which is what
     * lets a rejection scenario read the same as it does in the system test.
     */
    WhenPlaceOrder withQuantity(String quantity);

    WhenPlaceOrder withCountry(String country);

    WhenPlaceOrder withCouponCode(String couponCode);

    /** Uses the default coupon code — the one {@code given().coupon()} publishes when left unstated. */
    WhenPlaceOrder withCouponCode();
}
