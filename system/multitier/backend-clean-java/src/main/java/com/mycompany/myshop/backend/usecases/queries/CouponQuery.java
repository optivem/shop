package com.mycompany.myshop.backend.usecases.queries;

import java.util.List;

/**
 * The read side of coupons. See {@link OrderQuery} for why this is not a domain repository.
 */
public interface CouponQuery {

    /** Newest published first. */
    List<CouponListItem> listCoupons();
}
