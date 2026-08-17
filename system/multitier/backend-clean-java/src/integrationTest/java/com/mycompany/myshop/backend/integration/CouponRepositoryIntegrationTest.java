package com.mycompany.myshop.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.values.Rate;
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
            "SAVE20",
            Rate.of("0.2000"),
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-12-31T23:59:59Z"),
            100,
            7);

        couponRepository.save(coupon);
        forceDatabaseRoundTrip();

        var found = couponRepository.findByCode("SAVE20");
        assertThat(found).isPresent();
        assertThat(found.get().getDiscountRate()).isEqualTo(Rate.of("0.2000"));
        assertThat(found.get().getValidFrom()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(found.get().getValidTo()).isEqualTo(Instant.parse("2026-12-31T23:59:59Z"));
        assertThat(found.get().getUsageLimit()).isEqualTo(100);
        assertThat(found.get().getUsedCount()).isEqualTo(7);
    }

    /** An open-ended, unlimited coupon: three nullable columns come back null and stay legal. */
    @Test
    void savesAndReadsBackOpenEndedCoupon() {
        couponRepository.save(new Coupon("FOREVER", Rate.of("0.1000"), null, null, null, 0));
        forceDatabaseRoundTrip();

        var found = couponRepository.findByCode("FOREVER");
        assertThat(found).isPresent();
        assertThat(found.get().getValidFrom()).isNull();
        assertThat(found.get().getValidTo()).isNull();
        assertThat(found.get().getUsageLimit()).isNull();
        assertThat(found.get().getUsedCount()).isZero();
    }

    /**
     * A redemption re-saved through the port updates the existing row rather than inserting a second
     * one — the behaviour {@code CouponMapper} carries the surrogate id for.
     */
    @Test
    void redemptionUpdatesTheSameRow() {
        var coupon = couponRepository.save(new Coupon("ONCE", Rate.of("0.5000"), null, null, 1, 0));

        coupon.redeem();
        couponRepository.save(coupon);
        forceDatabaseRoundTrip();

        assertThat(couponRepository.findAll()).hasSize(1);
        assertThat(couponRepository.findByCode("ONCE")).get()
            .extracting(Coupon::getUsedCount).isEqualTo(1);
    }

    /** See {@code OrderRepositoryIntegrationTest#forceDatabaseRoundTrip} for why this is needed. */
    private void forceDatabaseRoundTrip() {
        entityManager.flush();
        entityManager.clear();
    }
}
