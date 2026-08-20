package com.mycompany.myshop.backend.usecases.order;

import com.mycompany.myshop.backend.usecases.queries.OrderCursor;

/**
 * A {@code null} {@code size} means "use the default"; anything else must be 1..{@code MAX_SIZE}. A
 * {@code null} {@code cursor} means the first page.
 *
 * <p>The cursor arrives typed. It reaches the wire as an opaque token, but decoding that token is
 * presentation's job -- a use case should not be able to tell which encoding the web layer chose.
 */
public record BrowseOrderHistoryRequest(String orderNumberFilter, Integer size, OrderCursor cursor) { }
