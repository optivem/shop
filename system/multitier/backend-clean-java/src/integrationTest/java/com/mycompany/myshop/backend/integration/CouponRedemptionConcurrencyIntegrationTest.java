package com.mycompany.myshop.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.UsageQuota;
import com.mycompany.myshop.backend.domain.values.ValidityPeriod;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CouponRedemptionConcurrencyIntegrationTest {

    private static final int RACERS = 2;
    private static final int TIMEOUT_SECONDS = 20;

    @FunctionalInterface
    private interface Attempt {
        boolean run(CyclicBarrier barrier) throws Exception;
    }

    @Autowired
    private CouponRepository couponRepository;

    @Test
    void readModifyWriteLosesAnUpdate() throws Exception {
        var code = givenCouponWithOneUnitLeft("RACE-RMW");

        var outcomes = race(barrier -> {
            var coupon = couponRepository.findByCode(code).orElseThrow();
            // Both racers have now read, and neither has written. This is the window, held open.
            awaitPeers(barrier);
            coupon.redeem();
            couponRepository.update(coupon);
            return true;
        });

        assertThat(outcomes).as("both racers believed they had redeemed").containsExactly(true, true);
        assertThat(usedCountOf(code)).as("the coupon counted only one of them").isEqualTo(1);
    }

    @Test
    void onlyOneOfTwoConcurrentRedemptionsSucceeds() throws Exception {
        var code = givenCouponWithOneUnitLeft("RACE-TRY");

        var outcomes = race(barrier -> {
            awaitPeers(barrier);
            return couponRepository.tryRedeem(code);
        });

        assertThat(outcomes).as("exactly one racer took the last unit")
                .containsExactlyInAnyOrder(true, false);
        assertThat(usedCountOf(code)).isEqualTo(1);
    }

    private CouponCode givenCouponWithOneUnitLeft(String value) {
        var code = CouponCode.of(value);
        couponRepository.add(new Coupon(code, Rate.of("0.5000"), ValidityPeriod.ALWAYS,
                UsageQuota.of(1, 0)));
        return code;
    }

    private int usedCountOf(CouponCode code) {
        return couponRepository.findByCode(code).orElseThrow().getQuota().used();
    }

    private List<Boolean> race(Attempt attempt) throws Exception {
        var barrier = new CyclicBarrier(RACERS);
        var pool = Executors.newFixedThreadPool(RACERS);
        try {
            var futures = new ArrayList<Future<Boolean>>();
            for (var racer = 0; racer < RACERS; racer++) {
                futures.add(pool.submit(() -> attempt.run(barrier)));
            }

            var outcomes = new ArrayList<Boolean>();
            for (var future : futures) {
                outcomes.add(future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    private static void awaitPeers(CyclicBarrier barrier) throws Exception {
        barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
