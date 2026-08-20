package com.mycompany.myshop.backend.presentation.controller;

import com.mycompany.myshop.backend.presentation.CursorCodec;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCouponsResponse;

import java.util.List;

/** See {@link BrowseOrderHistoryPageResponse}. The coupon cursor is a code, encoded the same way. */
public record BrowseCouponsPageResponse(
        List<BrowseCouponsResponse.BrowseCouponsItemResponse> coupons,
        String nextCursor,
        boolean hasMore) {

    static BrowseCouponsPageResponse of(BrowseCouponsResponse response, CursorCodec codec) {
        return new BrowseCouponsPageResponse(
                response.getCoupons(),
                codec.encodeCoupon(response.getNextCursor()),
                response.isHasMore());
    }
}
