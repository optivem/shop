package com.mycompany.myshop.testkit.dsl.core.scenario;

import java.util.Map;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class ExecutionResultContext {
    private static final ExecutionResultContext EMPTY = new ExecutionResultContext(null, null);

    private final String orderNumber;
    private final String couponCode;
    private final UnaryOperator<String> customerUsernames;
    /** The SKU of each order the operation placed, by order number, once looked up. */
    private Map<String, String> placedSkus;

    public ExecutionResultContext(String orderNumber, String couponCode) {
        this(orderNumber, couponCode, null);
    }

    public ExecutionResultContext(String orderNumber, String couponCode, UnaryOperator<String> customerUsernames) {
        this.orderNumber = orderNumber;
        this.couponCode = couponCode;
        this.customerUsernames = customerUsernames;
    }

    /** The username of the customer known in this scenario by the given alias, for assertions that name customers. */
    public String customerUsername(String customerAlias) {
        if (customerUsernames == null) {
            throw new IllegalStateException("The executed operation does not know this scenario's customers");
        }
        return customerUsernames.apply(customerAlias);
    }

    /** The SKU of each order the operation placed, by order number; looked up with {@code lookUp} the first time only. */
    public synchronized Map<String, String> placedSkus(Supplier<Map<String, String>> lookUp) {
        if (placedSkus == null) {
            placedSkus = lookUp.get();
        }
        return placedSkus;
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
