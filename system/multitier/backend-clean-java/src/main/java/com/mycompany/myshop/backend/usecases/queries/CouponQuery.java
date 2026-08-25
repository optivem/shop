package com.mycompany.myshop.backend.usecases.queries;

// The read side of coupons. See OrderQuery for why this is not a domain repository, and for
// the findAll() it replaced.
public interface CouponQuery {

    // Newest published first, one page at a time. The returned page carries the total number of
    // coupons, which is what a numbered-page client counts its buttons with.
    Page<CouponListItem> listCoupons(PageSpec page);
}
