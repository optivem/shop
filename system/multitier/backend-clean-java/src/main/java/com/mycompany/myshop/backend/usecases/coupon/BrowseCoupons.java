package com.mycompany.myshop.backend.usecases.coupon;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.usecases.dtos.BrowseCouponsResponse;

/**
 * Lists every published coupon. The domain → response mapping used to sit in the controller while
 * the order-history equivalent sat in the service; both now live in their use case, which is where
 * the response DTO is declared.
 */
public class BrowseCoupons {

    private final CouponRepository couponRepository;

    public BrowseCoupons(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public BrowseCouponsResponse execute() {
        var items = couponRepository.findAll().stream()
                .map(BrowseCoupons::toItem)
                .toList();

        var result = new BrowseCouponsResponse();
        result.setCoupons(items);
        return result;
    }

    private static BrowseCouponsResponse.BrowseCouponsItemResponse toItem(Coupon coupon) {
        var item = new BrowseCouponsResponse.BrowseCouponsItemResponse();
        item.setCode(coupon.getCode());
        item.setDiscountRate(coupon.getDiscountRate().value());
        item.setValidFrom(coupon.getValidFrom());
        item.setValidTo(coupon.getValidTo());
        item.setUsageLimit(coupon.getUsageLimit());
        item.setUsedCount(coupon.getUsedCount());
        return item;
    }
}
