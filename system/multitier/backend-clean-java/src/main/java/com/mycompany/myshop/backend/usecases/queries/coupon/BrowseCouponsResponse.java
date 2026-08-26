package com.mycompany.myshop.backend.usecases.queries.coupon;


import java.util.List;

// One page of coupons, exactly as it goes on the wire. The rows are the BrowseCouponsItemResponse projection
// itself, not a copy of it.
//
// See com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryResponse for what the
// four paging fields are, why totalPages is carried rather than derived, and why the read side does
// not re-map a row on its way out.
public record BrowseCouponsResponse(
        List<BrowseCouponsItemResponse> coupons,
        int page,
        int size,
        long totalElements,
        int totalPages) { }
