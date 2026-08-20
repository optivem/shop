package com.mycompany.myshop.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.UsageQuota;
import com.mycompany.myshop.backend.domain.values.ValidityPeriod;
import com.mycompany.myshop.backend.infrastructure.persistence.adapters.CouponRepositoryAdapter;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

// The Coupon half of the persistence adapter coverage; see OrderRepositoryIntegrationTest for why
// this layer exists at all and why @DataJpaTest is the right slice. backend-java has no counterpart
// to this class -- it had no CouponMapper, so there was nothing between the entity and the table.
//
// What is specific to Coupon is the nullable pair: validFrom/validTo are optional (an open-ended
// coupon) and usageLimit null means unlimited, so the mapper has three columns that must survive a
// null round trip without the domain constructor rejecting them on the way back.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, CouponRepositoryAdapter.class})
class CouponRepositoryIntegrationTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReadsBackCoupon() {
        var coupon = new Coupon(
            CouponCode.of("SAVE20"),
            Rate.of("0.2000"),
            new ValidityPeriod(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-12-31T23:59:59Z")),
            UsageQuota.of(100, 7));

        couponRepository.add(coupon);
        forceDatabaseRoundTrip();

        var found = couponRepository.findByCode(CouponCode.of("SAVE20"));
        assertThat(found).isPresent();
        assertThat(found.get().getDiscountRate()).isEqualTo(Rate.of("0.2000"));
        assertThat(found.get().getValidity()).isEqualTo(new ValidityPeriod(
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-12-31T23:59:59Z")));
        assertThat(found.get().getQuota()).isEqualTo(UsageQuota.of(100, 7));
    }

    @Test
    void savesAndReadsBackOpenEndedCoupon() {
        couponRepository.add(new Coupon(CouponCode.of("FOREVER"), Rate.of("0.1000"),
            ValidityPeriod.ALWAYS, UsageQuota.of(null, 0)));
        forceDatabaseRoundTrip();

        var found = couponRepository.findByCode(CouponCode.of("FOREVER"));
        assertThat(found).isPresent();
        assertThat(found.get().getValidity()).isEqualTo(ValidityPeriod.ALWAYS);
        assertThat(found.get().getQuota().limit()).isNull();
        assertThat(found.get().getQuota().used()).isZero();
    }

    @Test
    void redemptionUpdatesTheSameRow() {
        var coupon = new Coupon(CouponCode.of("ONCE"), Rate.of("0.5000"),
            ValidityPeriod.ALWAYS, UsageQuota.of(1, 0));
        couponRepository.add(coupon);

        coupon.redeem();
        couponRepository.update(coupon);
        forceDatabaseRoundTrip();

        // Counted straight off the table rather than through the port: findAll() was deleted with
        // Chunk R, and "the same row" is a claim about rows, not about the domain model.
        assertThat(entityManager.createQuery("SELECT COUNT(c) FROM CouponJpaEntity c", Long.class)
            .getSingleResult()).isEqualTo(1L);
        assertThat(couponRepository.findByCode(CouponCode.of("ONCE"))).get()
            .extracting(reread -> reread.getQuota().used()).isEqualTo(1);
    }

    private void forceDatabaseRoundTrip() {
        entityManager.flush();
        entityManager.clear();
    }
}
