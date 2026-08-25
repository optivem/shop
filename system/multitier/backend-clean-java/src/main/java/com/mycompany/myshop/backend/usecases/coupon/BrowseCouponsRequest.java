package com.mycompany.myshop.backend.usecases.coupon;

// See com.mycompany.myshop.backend.usecases.order.BrowseOrderHistoryRequest. Coupons have no
// filter to narrow the list by.
public record BrowseCouponsRequest(Integer page, Integer size) { }
