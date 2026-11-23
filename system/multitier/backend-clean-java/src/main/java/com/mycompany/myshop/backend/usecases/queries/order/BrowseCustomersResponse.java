package com.mycompany.myshop.backend.usecases.queries.order;

import java.util.List;

// The usernames that have placed an order, distinct, A to Z. The controller serves the list itself,
// so the wire shape is a bare JSON array, as in every other backend.
public record BrowseCustomersResponse(List<String> customers) { }
