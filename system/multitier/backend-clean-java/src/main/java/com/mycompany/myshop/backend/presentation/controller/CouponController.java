package com.mycompany.myshop.backend.presentation.controller;

import com.mycompany.myshop.backend.presentation.CursorCodec;
import com.mycompany.myshop.backend.presentation.InvalidCursorException;
import com.mycompany.myshop.backend.presentation.UseCaseResponder;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCouponsRequest;
import com.mycompany.myshop.backend.usecases.coupon.PublishCouponRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.mycompany.myshop.backend.usecases.coupon.BrowseCouponsResponse;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final UseCase<PublishCouponRequest, Void> publishCoupon;
    private final UseCase<BrowseCouponsRequest, BrowseCouponsResponse> browseCoupons;
    private final CursorCodec cursorCodec;
    private final UseCaseResponder responder;

    public CouponController(UseCase<PublishCouponRequest, Void> publishCoupon, UseCase<BrowseCouponsRequest, BrowseCouponsResponse> browseCoupons,
                            CursorCodec cursorCodec, UseCaseResponder responder) {
        this.publishCoupon = publishCoupon;
        this.browseCoupons = browseCoupons;
        this.cursorCodec = cursorCodec;
        this.responder = responder;
    }

    @PostMapping
    public ResponseEntity<Object> createCoupon(@Valid @RequestBody PublishCouponRequest request) {
        var result = publishCoupon.execute(request);
        return responder.respond(result, ignored -> ResponseEntity.noContent().build());
    }

    @GetMapping
    public ResponseEntity<Object> browseCoupons(@RequestParam(required = false) Integer size,
                                                @RequestParam(required = false) String cursor) {
        String decodedCursor;
        try {
            decodedCursor = cursorCodec.decodeCoupon(cursor);
        } catch (InvalidCursorException e) {
            return responder.badRequest(e.getMessage());
        }

        var result = browseCoupons.execute(new BrowseCouponsRequest(size, decodedCursor));
        return responder.respond(result, response ->
                ResponseEntity.ok(BrowseCouponsPageResponse.of(response, cursorCodec)));
    }
}
