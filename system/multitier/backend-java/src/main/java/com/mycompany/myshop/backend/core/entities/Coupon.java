package com.mycompany.myshop.backend.core.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal discountRate;

    @Column(name = "valid_from", nullable = true)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = true)
    private Instant validTo;

    @Column(name = "usage_limit", nullable = true)
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    @ColumnDefault("0")
    private Integer usedCount;

    public Coupon(String code, BigDecimal discountRate, Instant validFrom, Instant validTo, Integer usageLimit, Integer usedCount) {
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

    /**
     * Identity is the database id, so an entity stays equal to itself before and after it is persisted
     * and across Hibernate proxies; two unsaved entities are never equal. The hash code is constant per
     * class so it does not change when the id is assigned on save.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Coupon that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Coupon.class.hashCode();
    }
}
