package com.mycompany.myshop.api.controller;

import com.mycompany.myshop.core.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.core.dtos.PublishCouponRequest;
import com.mycompany.myshop.core.services.CouponService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
public class CouponApiController {

    private final CouponService couponService;

    public CouponApiController(CouponService couponService) {
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
    public BrowseCouponsResponse browseCoupons(@RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size) {
        return couponService.browseCoupons(page, size);
    }
}
