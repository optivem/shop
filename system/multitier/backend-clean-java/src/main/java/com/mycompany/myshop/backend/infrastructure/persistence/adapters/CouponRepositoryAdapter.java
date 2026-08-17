package com.mycompany.myshop.backend.infrastructure.persistence.adapters;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
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

    /** Resolves the row's generated key from the coupon's {@code code} — see {@link OrderRepositoryAdapter#save}. */
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
}
