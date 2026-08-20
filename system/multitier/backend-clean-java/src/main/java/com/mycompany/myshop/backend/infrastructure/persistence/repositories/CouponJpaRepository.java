package com.mycompany.myshop.backend.infrastructure.persistence.repositories;

import com.mycompany.myshop.backend.infrastructure.persistence.entities.CouponJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponJpaRepository extends JpaRepository<CouponJpaEntity, Long> {
    // Find coupon by code (business identifier)
    Optional<CouponJpaEntity> findByCode(String code);

    // The mutable half of a stored Coupon is its used count, so an update is that one column, keyed
    // on the code. Note what it is not: it writes the absolute count the caller read a moment ago,
    // so two callers who read the same value both write it and one redemption is lost. That is the
    // point of redeemIfAvailable below, and CouponRedemptionConcurrencyIntegrationTest demonstrates
    // the difference by using both.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CouponJpaEntity c SET c.usedCount = :usedCount WHERE c.code = :code")
    int updateUsedCount(@Param("code") String code, @Param("usedCount") int usedCount);

    // The other half of the check in Coupon#usageLimitReached: the check and the increment as one
    // statement, so nothing can happen between them. used_count = used_count + 1 reads the column
    // inside the write rather than trusting a value the application read earlier, and the WHERE clause
    // is the usage limit itself — so a row count of zero is not a failure to find the coupon, it is the
    // coupon saying no. A null usage_limit means unlimited, exactly as UsageQuota.exhausted() reads it.
    // Returns 1 when the use was recorded, 0 when the coupon is absent or already at its limit.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CouponJpaEntity c SET c.usedCount = c.usedCount + 1 "
            + "WHERE c.code = :code AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
    int redeemIfAvailable(@Param("code") String code);
}
