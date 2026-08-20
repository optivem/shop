package com.mycompany.myshop.backend.usecases.coupon;

// See com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryRequest. The coupon
// cursor is a coupon code, because a code is unique and already public -- no record needed to wrap
// a single column.
public record BrowseCouponsRequest(Integer size, String cursor) { }
