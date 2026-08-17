package com.mycompany.myshop.backend.infrastructure.persistence.mappers;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.UsageQuota;
import com.mycompany.myshop.backend.domain.values.ValidityPeriod;
import com.mycompany.myshop.backend.infrastructure.persistence.entities.CouponJpaEntity;

/**
 * Maps between the domain {@link Coupon} and its persisted shape. See {@link OrderMapper} for why
 * the surrogate {@code id} travels with the domain object.
 */
public final class CouponMapper {

    private CouponMapper() {
    }

    public static Coupon toDomain(CouponJpaEntity entity) {
        var coupon = new Coupon(
                CouponCode.of(entity.getCode()),
                Rate.of(entity.getDiscountRate()),
                new ValidityPeriod(entity.getValidFrom(), entity.getValidTo()),
                UsageQuota.of(entity.getUsageLimit(), entity.getUsedCount()));
        coupon.setId(entity.getId());
        return coupon;
    }

    public static CouponJpaEntity toEntity(Coupon coupon) {
        var entity = new CouponJpaEntity();
        entity.setId(coupon.getId());
        entity.setCode(coupon.getCode().value());
        entity.setDiscountRate(coupon.getDiscountRate().value());
        entity.setValidFrom(coupon.getValidity().validFrom());
        entity.setValidTo(coupon.getValidity().validTo());
        entity.setUsageLimit(coupon.getQuota().limit());
        entity.setUsedCount(coupon.getQuota().used());
        return entity;
    }
}
