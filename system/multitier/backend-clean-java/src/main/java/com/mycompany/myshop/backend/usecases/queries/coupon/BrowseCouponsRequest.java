package com.mycompany.myshop.backend.usecases.queries.coupon;

// See com.mycompany.myshop.backend.usecases.queries.order.BrowseOrderHistoryRequest. Coupons have no
// filter to narrow the list by.
public record BrowseCouponsRequest(Integer page, Integer size) { }
