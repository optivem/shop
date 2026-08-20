package com.mycompany.myshop.backend.usecases.queries;

/**
 * The read side of coupons. See {@link OrderQuery} for why this is not a domain repository, and for
 * the {@code findAll()} it replaced.
 */
public interface CouponQuery {

    /**
     * Newest published first, one page at a time. The cursor is the code of the last coupon on the
     * previous page; {@code null} means "start at the newest". A code rather than a position,
     * because a coupon published while the client is paging must not shift every later page by one.
     */
    Page<CouponListItem> listCoupons(PageSpec<String> page);
}
