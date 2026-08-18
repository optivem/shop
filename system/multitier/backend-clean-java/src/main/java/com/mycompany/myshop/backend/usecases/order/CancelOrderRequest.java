package com.mycompany.myshop.backend.usecases.order;

/**
 * What cancelling an order needs. See {@link ViewOrderDetailsRequest} on why this is a record.
 */
public record CancelOrderRequest(String orderNumber) { }
