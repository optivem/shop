package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.values.Rate;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A coupon. A plain object: no ORM annotations, no Spring, no Lombok — the persisted shape lives in
 * {@code infrastructure.persistence.entities.CouponJpaEntity}.
 *
 * <p>It decides for itself whether it can be redeemed. {@link #discountAt(Instant)} holds the
 * validity window and the usage limit that used to be four {@code if} branches in the coupon
 * service, and {@link #redeem()} is the only way the used count moves.
 */
public class Coupon {

    private static final String FIELD_COUPON_CODE = "couponCode";
    private static final String MSG_COUPON_NOT_YET_VALID = "Coupon code %s is not yet valid";
    private static final String MSG_COUPON_EXPIRED = "Coupon code %s has expired";
    private static final String MSG_COUPON_USAGE_LIMIT_REACHED = "Coupon code %s has exceeded its usage limit";

    private Long id;
    private final String code;
    private final Rate discountRate;
    private final Instant validFrom;
    private final Instant validTo;
    private final Integer usageLimit;
    private int usedCount;

    public Coupon(String code, Rate discountRate, Instant validFrom, Instant validTo,
                  Integer usageLimit, Integer usedCount) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("code cannot be null or empty");
        }
        if (discountRate == null) {
            throw new IllegalArgumentException("discountRate cannot be null");
        }
        if (discountRate.value().compareTo(BigDecimal.ZERO) <= 0
                || discountRate.value().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("discountRate must be greater than 0 and at most 1");
        }
        // Only validate date order if both dates are provided
        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
        // usageLimit is optional - null means unlimited
        if (usageLimit != null && usageLimit < 0) {
            throw new IllegalArgumentException("usageLimit must be non-negative");
        }
        if (usedCount == null) {
            throw new IllegalArgumentException("usedCount cannot be null");
        }
        if (usedCount < 0) {
            throw new IllegalArgumentException("usedCount must be non-negative");
        }

        this.code = code;
        this.discountRate = discountRate;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount;
    }

    /**
     * The discount this coupon grants at {@code at}.
     *
     * @throws ValidationException when the coupon is not yet valid, has expired, or has been used
     *                             as often as it may be.
     */
    public Rate discountAt(Instant at) {
        if (validFrom != null && at.isBefore(validFrom)) {
            throw reject(MSG_COUPON_NOT_YET_VALID);
        }
        if (validTo != null && at.isAfter(validTo)) {
            throw reject(MSG_COUPON_EXPIRED);
        }
        if (usageLimit != null && usedCount >= usageLimit) {
            throw reject(MSG_COUPON_USAGE_LIMIT_REACHED);
        }

        return discountRate;
    }

    /** Records one use of this coupon. */
    public void redeem() {
        usedCount++;
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

    public Instant getValidFrom() {
        return validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }
}
