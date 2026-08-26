package com.mycompany.myshop.backend.presentation.controllers;

import com.mycompany.myshop.backend.presentation.UseCaseResponder;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsRequest;
import com.mycompany.myshop.backend.usecases.commands.coupon.PublishCouponRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mycompany.myshop.backend.usecases.queries.coupon.BrowseCouponsResponse;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final UseCase<PublishCouponRequest, Void> publishCoupon;
    private final UseCase<BrowseCouponsRequest, BrowseCouponsResponse> browseCoupons;
    private final UseCaseResponder responder;

    public CouponController(UseCase<PublishCouponRequest, Void> publishCoupon, UseCase<BrowseCouponsRequest, BrowseCouponsResponse> browseCoupons,
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
    public ResponseEntity<Object> browseCoupons(@RequestParam(required = false) Integer page,
                                                @RequestParam(required = false) Integer size) {
        var result = browseCoupons.execute(new BrowseCouponsRequest(page, size));
        return responder.respond(result, response ->
                ResponseEntity.ok(response));
    }
}
