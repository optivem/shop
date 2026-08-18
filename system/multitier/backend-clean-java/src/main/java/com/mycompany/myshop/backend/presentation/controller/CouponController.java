package com.mycompany.myshop.backend.presentation.controller;

import com.mycompany.myshop.backend.presentation.UseCaseResponder;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCoupons;
import com.mycompany.myshop.backend.usecases.coupon.PublishCoupon;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCouponsRequest;
import com.mycompany.myshop.backend.usecases.coupon.PublishCouponRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final PublishCoupon publishCoupon;
    private final BrowseCoupons browseCoupons;
    private final UseCaseResponder responder;

    public CouponController(PublishCoupon publishCoupon, BrowseCoupons browseCoupons,
                            UseCaseResponder responder) {
        this.publishCoupon = publishCoupon;
        this.browseCoupons = browseCoupons;
        this.responder = responder;
    }

    @PostMapping
    public ResponseEntity<Object> createCoupon(@Valid @RequestBody PublishCouponRequest request) {
        var result = publishCoupon.execute(request);
        return responder.respond(result, ignored -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public ResponseEntity<Object> browseCoupons() {
        var result = browseCoupons.execute(new BrowseCouponsRequest());
        return responder.respond(result, ResponseEntity::ok);
    }
}
