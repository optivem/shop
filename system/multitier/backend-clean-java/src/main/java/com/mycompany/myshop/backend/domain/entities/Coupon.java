package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.UsageQuota;
import com.mycompany.myshop.backend.domain.values.ValidityPeriod;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A coupon. A plain object: no ORM annotations, no Spring, no Lombok — the persisted shape lives in
 * {@code infrastructure.persistence.entities.CouponJpaEntity}.
 *
 * <p>It decides for itself whether it can be redeemed. {@link #discountAt(Instant)} asks its
 * {@link ValidityPeriod} and its {@link UsageQuota} — the four {@code if} branches that used to be in
 * the coupon service, and then the four nullable fields that used to be here — and {@link #redeem()}
 * is the only way the used count moves.
 */
public class Coupon {

    private static final String FIELD_COUPON_CODE = "couponCode";
    private static final String MSG_COUPON_NOT_YET_VALID = "Coupon code %s is not yet valid";
    private static final String MSG_COUPON_EXPIRED = "Coupon code %s has expired";
    private static final String MSG_COUPON_USAGE_LIMIT_REACHED = "Coupon code %s has exceeded its usage limit";

    private Long id;
    private final String code;
    private final Rate discountRate;
    private final ValidityPeriod validity;
    private UsageQuota quota;

    public Coupon(String code, Rate discountRate, ValidityPeriod validity, UsageQuota quota) {
        Guard.notNullOrEmpty(code, "code");
        Guard.notNull(discountRate, "discountRate");
        Guard.notNull(validity, "validity");
        Guard.notNull(quota, "quota");
        // A coupon's own rule, not a general one about rates: a tax rate of zero is legal, a coupon
        // that discounts nothing is not.
        if (discountRate.value().compareTo(BigDecimal.ZERO) <= 0
                || discountRate.value().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discountRate must be greater than 0 and at most 1");
        }

        this.code = code;
        this.discountRate = discountRate;
        this.validity = validity;
        this.quota = quota;
    }

    /**
     * The discount this coupon grants at {@code at}.
     *
     * @throws ValidationException when the coupon is not yet valid, has expired, or has been used
     *                             as often as it may be.
     */
    public Rate discountAt(Instant at) {
        if (validity.notYetValidAt(at)) {
            throw reject(MSG_COUPON_NOT_YET_VALID);
        }
        if (validity.expiredAt(at)) {
            throw reject(MSG_COUPON_EXPIRED);
        }
        if (quota.exhausted()) {
            throw reject(MSG_COUPON_USAGE_LIMIT_REACHED);
        }

        return discountRate;
    }

    /** Records one use of this coupon. */
    public void redeem() {
        quota = quota.recordUse();
    }

    private ValidationException reject(String messageFormat) {
        return new ValidationException(FIELD_COUPON_CODE, String.format(messageFormat, code));
    }

    public Long getId() {
        return id;
    }

    /** Set by the repository adapter once storage has assigned the row its identity. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
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
