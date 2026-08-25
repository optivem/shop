package com.mycompany.myshop.backend.presentation.controller;

import com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryResponse;

import java.util.List;

// The wire shape of a page of orders: the use case's list, plus the four facts a numbered-page
// client needs to draw its controls.
//
// It exists so the paging metadata sits beside the rows rather than inside them. The item list is
// passed through by reference rather than re-mapped field by field: the items were already the wire
// contract.
public record BrowseOrderHistoryPageResponse(
        List<BrowseOrderHistoryResponse.BrowseOrderHistoryItemResponse> orders,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    static BrowseOrderHistoryPageResponse of(BrowseOrderHistoryResponse response) {
        return new BrowseOrderHistoryPageResponse(
                response.getOrders(),
                response.getPage(),
                response.getSize(),
                response.getTotalElements(),
                response.getTotalPages());
    }
}
