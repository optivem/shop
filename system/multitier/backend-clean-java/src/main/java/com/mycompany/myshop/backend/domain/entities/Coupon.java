package com.mycompany.myshop.backend.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A coupon. A plain object: no ORM annotations, no Spring, no Lombok — the persisted shape lives in
 * {@code infrastructure.persistence.entities.CouponJpaEntity}.
 *
 * <p>Validity and redemption are still decided in {@code CouponService}; Step 8 of the refactor plan
 * moves them onto this class.
 */
public class Coupon {

    private Long id;
    private String code;
    private BigDecimal discountRate;
    private Instant validFrom;
    private Instant validTo;
    private Integer usageLimit;
    private Integer usedCount;

    public Coupon() {
    }

    public Coupon(String code, BigDecimal discountRate, Instant validFrom, Instant validTo,
                  Integer usageLimit, Integer usedCount) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("code cannot be null or empty");
        }
        if (discountRate == null) {
            throw new IllegalArgumentException("discountRate cannot be null");
        }
        if (discountRate.compareTo(BigDecimal.ZERO) <= 0 || discountRate.compareTo(BigDecimal.ONE) > 0) {
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }
}
