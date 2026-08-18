package com.mycompany.myshop.backend.domain.repositories;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.values.CouponCode;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {

    Coupon save(Coupon coupon);

    Optional<Coupon> findByCode(CouponCode code);

    List<Coupon> findAll();

    boolean tryRedeem(CouponCode code);
}
