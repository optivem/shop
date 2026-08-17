package com.mycompany.myshop.backend.core.services;

import com.mycompany.myshop.backend.core.entities.Coupon;
import com.mycompany.myshop.backend.core.exceptions.ValidationException;
import com.mycompany.myshop.backend.core.repositories.CouponRepository;
import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class CouponService {

    private static final String FIELD_COUPON_CODE = "couponCode";
    private static final String MSG_COUPON_DOES_NOT_EXIST = "Coupon code %s does not exist";
    private static final String MSG_COUPON_NOT_YET_VALID = "Coupon code %s is not yet valid";
    private static final String MSG_COUPON_EXPIRED = "Coupon code %s has expired";
    private static final String MSG_COUPON_USAGE_LIMIT_REACHED = "Coupon code %s has exceeded its usage limit";
    private static final String MSG_COUPON_CODE_ALREADY_EXISTS = "Coupon code %s already exists";

    private final CouponRepository couponRepository;
    private final ClockGateway clockGateway;

    public CouponService(CouponRepository couponRepository, ClockGateway clockGateway) {
        this.couponRepository = couponRepository;
        this.clockGateway = clockGateway;
    }

    public BigDecimal getDiscount(String couponCode) {
        // No coupon provided, no discount
        if (couponCode == null || couponCode.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        var optionalCoupon = couponRepository.findByCode(couponCode);

        if (optionalCoupon.isEmpty()) {
            throwCouponValidationException(MSG_COUPON_DOES_NOT_EXIST, couponCode);
        }

        var coupon = optionalCoupon.get();
        var now = clockGateway.getCurrentTime();

        // If validFrom is set and current time is before it, coupon is not yet valid
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            throwCouponValidationException(MSG_COUPON_NOT_YET_VALID, couponCode);
        }

        // If validTo is set and current time is after it, coupon has expired
        if (coupon.getValidTo() != null && now.isAfter(coupon.getValidTo())) {
            throwCouponValidationException(MSG_COUPON_EXPIRED, couponCode);
        }

        // Check usage limit only if it's set (not null)
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throwCouponValidationException(MSG_COUPON_USAGE_LIMIT_REACHED, couponCode);
        }

        return coupon.getDiscountRate();
    }

    public void incrementUsageCount(String couponCode) {
        var optionalCoupon = couponRepository.findByCode(couponCode);
        if (optionalCoupon.isPresent()) {
            var coupon = optionalCoupon.get();
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }
    }

    public Coupon createCoupon(String couponCode, BigDecimal discountRate, Instant validFrom, Instant validTo, Integer usageLimit) {
        if (couponRepository.findByCode(couponCode).isPresent()) {
            throwCouponValidationException(MSG_COUPON_CODE_ALREADY_EXISTS, couponCode);
        }

        // If usageLimit is null, set to unlimited (Integer.MAX_VALUE)
        int limit = usageLimit != null ? usageLimit : Integer.MAX_VALUE;
        var coupon = new Coupon(couponCode, discountRate, validFrom, validTo, limit, 0);
        return couponRepository.save(coupon);
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    private void throwCouponValidationException(String messageFormat, String couponCode) {
        throw new ValidationException(FIELD_COUPON_CODE, String.format(messageFormat, couponCode));
    }
}
