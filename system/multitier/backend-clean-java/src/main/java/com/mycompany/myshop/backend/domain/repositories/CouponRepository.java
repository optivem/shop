package com.mycompany.myshop.backend.domain.repositories;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.values.CouponCode;

import java.util.Optional;

public interface CouponRepository {

    // See OrderRepository for why this is not one `save`.
    void add(Coupon coupon);

    // Last-writer-wins on the count it is given, which is exactly why it is not how a redemption is
    // recorded: tryRedeem is. Kept because the read-modify-write path is worth being able to
    // demonstrate losing an update.
    void update(Coupon coupon);

    Optional<Coupon> findByCode(CouponCode code);

    boolean tryRedeem(CouponCode code);
}
