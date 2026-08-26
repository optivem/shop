package com.mycompany.myshop.backend.usecases.queries.coupon.ports;
import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsItemResponse;
import com.mycompany.myshop.backend.usecases.queries.common.Page;import com.mycompany.myshop.backend.usecases.queries.common.PageSpec;


// The read side of coupons. See OrderReader for why this is not a domain repository, and for
// the findAll() it replaced.
public interface CouponReader {

    // Newest published first, one page at a time. The returned page carries the total number of
    // coupons, which is what a numbered-page client counts its buttons with.
    Page<BrowseCouponsItemResponse> listCoupons(PageSpec page);
}
