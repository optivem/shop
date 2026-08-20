package com.mycompany.myshop.backend.infrastructure.persistence.adapters;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.infrastructure.persistence.mappers.CouponMapper;
import com.mycompany.myshop.backend.infrastructure.persistence.repositories.CouponJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class CouponRepositoryAdapter implements CouponRepository {

    private final CouponJpaRepository jpaRepository;

    public CouponRepositoryAdapter(CouponJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    // See OrderRepositoryAdapter#add: no id on the mapped entity means one INSERT and no lookup. The
    // caller that publishes a coupon has already established the code is free, one line earlier.
    @Override
    public void add(Coupon coupon) {
        jpaRepository.save(CouponMapper.toEntity(coupon));
    }

    @Transactional
    @Override
    public void update(Coupon coupon) {
        jpaRepository.updateUsedCount(coupon.getCode().value(), coupon.getQuota().used());
    }

    @Override
    public Optional<Coupon> findByCode(CouponCode code) {
        return jpaRepository.findByCode(code.value()).map(CouponMapper::toDomain);
    }

    @Transactional
    @Override
    public boolean tryRedeem(CouponCode code) {
        return jpaRepository.redeemIfAvailable(code.value()) == 1;
    }
}
