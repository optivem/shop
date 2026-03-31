package com.optivem.eshop.dsl.core.scenario;

public class ExecutionResultContext {
    private static final ExecutionResultContext EMPTY = new ExecutionResultContext(null);

    private final String orderNumber;

    public ExecutionResultContext(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public static ExecutionResultContext empty() {
        return EMPTY;
    }

    public String getOrderNumber() {
        return orderNumber;
    }
}

