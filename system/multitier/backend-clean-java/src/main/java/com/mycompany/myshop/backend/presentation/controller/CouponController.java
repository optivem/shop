package com.mycompany.myshop.backend.presentation.controller;

import com.mycompany.myshop.backend.usecases.coupon.BrowseCoupons;
import com.mycompany.myshop.backend.usecases.coupon.PublishCoupon;
import com.mycompany.myshop.backend.usecases.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.backend.usecases.dtos.PublishCouponRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Binds HTTP to use cases and nothing else — the coupon → response mapping that used to sit in
 * {@code browseCoupons} now lives in {@link BrowseCoupons}, alongside the DTO it fills in.
 */
@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final PublishCoupon publishCoupon;
    private final BrowseCoupons browseCoupons;

    public CouponController(PublishCoupon publishCoupon, BrowseCoupons browseCoupons) {
        this.publishCoupon = publishCoupon;
        this.browseCoupons = browseCoupons;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createCoupon(@Valid @RequestBody PublishCouponRequest request) {
        publishCoupon.execute(request);
    }

    @GetMapping
    public BrowseCouponsResponse browseCoupons() {
        return browseCoupons.execute();
    }
}
