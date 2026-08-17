package com.mycompany.myshop.backend.api.controller;

import com.mycompany.myshop.backend.core.dtos.PublishCouponRequest;
import com.mycompany.myshop.backend.core.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.backend.core.services.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createCoupon(@Valid @RequestBody PublishCouponRequest request) {
        couponService.createCoupon(
                request.getCode(),
                request.getDiscountRate(),
                request.getValidFrom(),
                request.getValidTo(),
                request.getUsageLimit()
        );
    }

    @GetMapping
    public BrowseCouponsResponse browseCoupons() {
        var items = couponService.getAllCoupons().stream()
                .map(coupon -> {
                    var response = new BrowseCouponsResponse.BrowseCouponsItemResponse();
                    response.setCode(coupon.getCode());
                    response.setDiscountRate(coupon.getDiscountRate());
                    response.setValidFrom(coupon.getValidFrom());
                    response.setValidTo(coupon.getValidTo());
                    response.setUsageLimit(coupon.getUsageLimit());
                    response.setUsedCount(coupon.getUsedCount());
                    return response;
                })
                .toList();

        var result = new BrowseCouponsResponse();
        result.setCoupons(items);
        return result;
    }
}
