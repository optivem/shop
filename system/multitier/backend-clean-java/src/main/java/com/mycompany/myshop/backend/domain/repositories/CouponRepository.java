package com.mycompany.myshop.backend.domain.repositories;

import com.mycompany.myshop.backend.domain.entities.Coupon;

import java.util.List;
import java.util.Optional;

/**
 * The port to coupon storage. A plain interface — no Spring Data, no JPA. Implemented by
 * {@code infrastructure.persistence.adapters.CouponRepositoryAdapter}.
 */
public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findByCode(String code);

    List<Coupon> findAll();
}
