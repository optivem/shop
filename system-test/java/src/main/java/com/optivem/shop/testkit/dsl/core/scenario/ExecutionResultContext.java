package com.optivem.shop.testkit.dsl.core.scenario;

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
