package com.mycompany.myshop.backend.usecases.coupon;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import com.mycompany.myshop.backend.usecases.dtos.BrowseCouponsRequest;
import com.mycompany.myshop.backend.usecases.dtos.BrowseCouponsResponse;

/**
 * Lists every published coupon. The domain → response mapping used to sit in the controller while
 * the order-history equivalent sat in the service; both now live in their use case, which is where
 * the response DTO is declared.
 */
public class BrowseCoupons implements UseCase<BrowseCouponsRequest, BrowseCouponsResponse> {

    private final CouponRepository couponRepository;

    public BrowseCoupons(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public Result<BrowseCouponsResponse, UseCaseError> execute(BrowseCouponsRequest request) {
        var items = couponRepository.findAll().stream()
                .map(BrowseCoupons::toItem)
                .toList();

        var response = new BrowseCouponsResponse();
        response.setCoupons(items);
        return Result.ok(response);
    }

    private static BrowseCouponsResponse.BrowseCouponsItemResponse toItem(Coupon coupon) {
        var item = new BrowseCouponsResponse.BrowseCouponsItemResponse();
        item.setCode(coupon.getCode().value());
        item.setDiscountRate(coupon.getDiscountRate().value());
        item.setValidFrom(coupon.getValidity().validFrom());
        item.setValidTo(coupon.getValidity().validTo());
        item.setUsageLimit(coupon.getQuota().limit());
        item.setUsedCount(coupon.getQuota().used());
        return item;
    }
}
