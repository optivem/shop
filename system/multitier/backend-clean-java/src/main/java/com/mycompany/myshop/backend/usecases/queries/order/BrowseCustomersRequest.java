package com.mycompany.myshop.backend.usecases.queries.order;

// The customer list is unpaged and unfiltered, so there is nothing to ask for. The record exists
// because every use case takes one request in.
public record BrowseCustomersRequest() { }
