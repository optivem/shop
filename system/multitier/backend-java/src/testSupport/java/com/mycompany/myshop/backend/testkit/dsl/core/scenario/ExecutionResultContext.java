package com.mycompany.myshop.backend.support.core.scenario;

/**
 * What the action left behind for {@code then()} to resolve against: the order number the SUT
 * generated, and the coupon code the action carried. This is what lets {@code then().order()} and
 * {@code then().coupon()} take no argument.
 */
public class ExecutionResultContext {

    private static final ExecutionResultContext EMPTY = new ExecutionResultContext(null, null);

    private final String orderNumber;
    private final String couponCode;

    public ExecutionResultContext(String orderNumber, String couponCode) {
        this.orderNumber = orderNumber;
        this.couponCode = couponCode;
    }

    public static ExecutionResultContext empty() {
        return EMPTY;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getCouponCode() {
        return couponCode;
    }
}
