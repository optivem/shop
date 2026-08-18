package com.mycompany.myshop.backend.usecases.order;

/**
 * What viewing one order needs. A record rather than a bean like {@link PlaceOrderRequest}: nothing
 * deserializes this from a body — the controller builds it from the path.
 */
public record ViewOrderDetailsRequest(String orderNumber) { }
