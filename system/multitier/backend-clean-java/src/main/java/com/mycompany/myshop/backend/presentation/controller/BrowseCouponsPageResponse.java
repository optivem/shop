package com.mycompany.myshop.backend.presentation.controller;

import com.mycompany.myshop.backend.usecases.coupon.BrowseCouponsResponse;

import java.util.List;

// See BrowseOrderHistoryPageResponse. Coupons page the same way.
public record BrowseCouponsPageResponse(
        List<BrowseCouponsResponse.BrowseCouponsItemResponse> coupons,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    static BrowseCouponsPageResponse of(BrowseCouponsResponse response) {
        return new BrowseCouponsPageResponse(
                response.getCoupons(),
                response.getPage(),
                response.getSize(),
                response.getTotalElements(),
                response.getTotalPages());
    }
}
