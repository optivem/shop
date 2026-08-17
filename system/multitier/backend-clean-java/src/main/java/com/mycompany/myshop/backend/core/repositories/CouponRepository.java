package com.mycompany.myshop.backend.core.repositories;

import com.mycompany.myshop.backend.core.entities.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    // Find coupon by code (business identifier)
    Optional<Coupon> findByCode(String code);
}
