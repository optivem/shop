package com.mycompany.myshop.backend.infrastructure.persistence.adapters;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.infrastructure.persistence.mappers.CouponMapper;
import com.mycompany.myshop.backend.infrastructure.persistence.repositories.CouponJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Implements the domain's {@link CouponRepository} port with Spring Data JPA.
 */
@Component
public class CouponRepositoryAdapter implements CouponRepository {

    private final CouponJpaRepository jpaRepository;

    public CouponRepositoryAdapter(CouponJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        var saved = jpaRepository.save(CouponMapper.toEntity(coupon));
        coupon.setId(saved.getId());
        return CouponMapper.toDomain(saved);
    }

    @Override
    public Optional<Coupon> findByCode(String code) {
        return jpaRepository.findByCode(code).map(CouponMapper::toDomain);
    }

    @Override
    public List<Coupon> findAll() {
        return jpaRepository.findAll().stream()
                .map(CouponMapper::toDomain)
                .toList();
    }
}
