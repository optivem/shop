package com.mycompany.myshop.backend.infrastructure.persistence.mappers;

import com.mycompany.myshop.backend.domain.entities.Coupon;
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
                entity.getCode(),
                entity.getDiscountRate(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getUsageLimit(),
                entity.getUsedCount());
        coupon.setId(entity.getId());
        return coupon;
    }

    public static CouponJpaEntity toEntity(Coupon coupon) {
        var entity = new CouponJpaEntity();
        entity.setId(coupon.getId());
        entity.setCode(coupon.getCode());
        entity.setDiscountRate(coupon.getDiscountRate());
        entity.setValidFrom(coupon.getValidFrom());
        entity.setValidTo(coupon.getValidTo());
        entity.setUsageLimit(coupon.getUsageLimit());
        entity.setUsedCount(coupon.getUsedCount());
        return entity;
    }
}
