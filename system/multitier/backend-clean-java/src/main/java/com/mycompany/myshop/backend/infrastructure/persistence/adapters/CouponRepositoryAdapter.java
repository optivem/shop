package com.mycompany.myshop.backend.infrastructure.persistence.adapters;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.infrastructure.persistence.mappers.CouponMapper;
import com.mycompany.myshop.backend.infrastructure.persistence.repositories.CouponJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class CouponRepositoryAdapter implements CouponRepository {

    private final CouponJpaRepository jpaRepository;

    public CouponRepositoryAdapter(CouponJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        var entity = CouponMapper.toEntity(coupon);
        jpaRepository.findByCode(coupon.getCode().value())
                .ifPresent(existing -> entity.setId(existing.getId()));
        return CouponMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Coupon> findByCode(CouponCode code) {
        return jpaRepository.findByCode(code.value()).map(CouponMapper::toDomain);
    }

    @Override
    public List<Coupon> findAll() {
        return jpaRepository.findAll().stream()
                .map(CouponMapper::toDomain)
                .toList();
    }

    @Transactional
    @Override
    public boolean tryRedeem(CouponCode code) {
        return jpaRepository.redeemIfAvailable(code.value()) == 1;
    }
}
