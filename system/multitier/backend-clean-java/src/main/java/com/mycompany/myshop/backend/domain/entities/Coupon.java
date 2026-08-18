package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.UsageQuota;
import com.mycompany.myshop.backend.domain.values.ValidityPeriod;

import java.time.Instant;

public class Coupon {

    private static final String MSG_COUPON_NOT_YET_VALID = "Coupon code %s is not yet valid";
    private static final String MSG_COUPON_EXPIRED = "Coupon code %s has expired";
    private static final String MSG_COUPON_USAGE_LIMIT_REACHED = "Coupon code %s has exceeded its usage limit";

    private final CouponCode code;
    private final Rate discountRate;
    private final ValidityPeriod validity;
    private UsageQuota quota;

    public Coupon(CouponCode code, Rate discountRate, ValidityPeriod validity, UsageQuota quota) {
        Guard.notNull(code, "code");
        Guard.notNull(discountRate, "discountRate");
        Guard.notNull(validity, "validity");
        Guard.notNull(quota, "quota");
        // A coupon's own rule, not a general one about rates: a tax rate of zero is legal, a coupon
        // that discounts nothing is not.
        if (!discountRate.isPositive() || discountRate.isGreaterThan(Rate.ONE)) {
            throw new IllegalArgumentException("discountRate must be greater than 0 and at most 1");
        }

        this.code = code;
        this.discountRate = discountRate;
        this.validity = validity;
        this.quota = quota;
    }

    public Rate discountAt(Instant at) {
        if (validity.notYetValidAt(at)) {
            throw reject(MSG_COUPON_NOT_YET_VALID);
        }
        if (validity.expiredAt(at)) {
            throw reject(MSG_COUPON_EXPIRED);
        }
        if (quota.exhausted()) {
            throw usageLimitReached(code);
        }

        return discountRate;
    }

    // Static and public because the usage-limit rule is checked twice on purpose: here in memory on
    // the read path, so discountAt fails fast with a good message before anything is priced, and again
    // in storage as the WHERE clause of a conditional update (CouponRepository#tryRedeem), because
    // between the read and the write another request can take the last unit. The database is
    // authoritative, memory is the fast path — two checks, one wording.
    public static ValidationException usageLimitReached(CouponCode code) {
        return new ValidationException(CouponCode.FIELD_NAME,
                String.format(MSG_COUPON_USAGE_LIMIT_REACHED, code));
    }

    public void redeem() {
        quota = quota.recordUse();
    }

    private ValidationException reject(String messageFormat) {
        return new ValidationException(CouponCode.FIELD_NAME, String.format(messageFormat, code));
    }

    public CouponCode getCode() {
        return code;
    }

    public Rate getDiscountRate() {
        return discountRate;
    }

    public ValidityPeriod getValidity() {
        return validity;
    }

    public UsageQuota getQuota() {
        return quota;
    }
}
