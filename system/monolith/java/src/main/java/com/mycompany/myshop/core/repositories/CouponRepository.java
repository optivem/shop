package com.mycompany.myshop.core.repositories;

import com.mycompany.myshop.core.entities.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCode(String code);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Coupon c SET c.usedCount = c.usedCount + 1 WHERE c.code = :code AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    int incrementUsedCountIfBelowLimit(@Param("code") String code);
}
