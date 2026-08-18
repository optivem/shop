package com.mycompany.myshop.backend.usecases.dtos;

/**
 * What delivering an order needs. See {@link ViewOrderDetailsRequest} on why this is a record.
 */
public record DeliverOrderRequest(String orderNumber) { }
