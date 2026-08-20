package com.mycompany.myshop.backend.presentation.controller;

import com.mycompany.myshop.backend.presentation.CursorCodec;
import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryResponse;

import java.util.List;

/**
 * The wire shape of a page of orders: the use case's list, plus the two facts a paging client needs.
 *
 * <p>It exists so the cursor reaches the client encoded. The use case's own response holds a typed
 * {@code OrderCursor}, and serializing that directly would publish the sort key as a JSON object --
 * exactly what the opaque token avoids. The item list is passed through by reference rather than
 * re-mapped field by field: the items were already the wire contract.
 */
public record BrowseOrderHistoryPageResponse(
        List<BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse> orders,
        String nextCursor,
        boolean hasMore) {

    static BrowseOrderHistoryPageResponse of(BrowseOrderHistoryResponse response, CursorCodec codec) {
        return new BrowseOrderHistoryPageResponse(
                response.getOrders(),
                codec.encodeOrder(response.getNextCursor()),
                response.isHasMore());
    }
}
