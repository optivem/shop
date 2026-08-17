package com.mycompany.myshop.backend.infrastructure.persistence.repositories;

import com.mycompany.myshop.backend.infrastructure.persistence.entities.CouponJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponJpaRepository extends JpaRepository<CouponJpaEntity, Long> {
    // Find coupon by code (business identifier)
    Optional<CouponJpaEntity> findByCode(String code);
}
