package com.mycompany.myshop.backend.usecases.dtos;

/**
 * What browsing order history needs. {@code orderNumberFilter} is nullable — absent means every
 * order. See {@link ViewOrderDetailsRequest} on why this is a record.
 */
public record BrowseOrderHistoryRequest(String orderNumberFilter) { }
