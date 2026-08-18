package com.mycompany.myshop.backend.domain.values;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class UsageQuotaTest {

    @Test
    void isNotExhaustedBelowTheLimit() {
        assertThat(UsageQuota.of(3, 2).exhausted()).isFalse();
    }

    @Test
    void isExhaustedOnceUsageReachesTheLimit() {
        assertThat(UsageQuota.of(3, 3).exhausted()).isTrue();
    }

    @Test
    void isExhaustedBeyondTheLimit() {
        assertThat(UsageQuota.of(3, 5).exhausted()).isTrue();
    }

    @Test
    void isNeverExhaustedWithoutALimit() {
        assertThat(UsageQuota.of(null, 0).exhausted()).isFalse();
        assertThat(UsageQuota.of(null, 1_000_000).exhausted()).isFalse();
    }

    @Test
    void isExhaustedImmediatelyWithAZeroLimit() {
        assertThat(UsageQuota.of(0, 0).exhausted()).isTrue();
    }

    @Test
    void recordUseYieldsTheNextCountAndLeavesTheOriginalUntouched() {
        var quota = UsageQuota.of(3, 1);

        var next = quota.recordUse();

        assertThat(next.used()).isEqualTo(2);
        assertThat(next.limit()).isEqualTo(3);
        assertThat(quota.used()).isEqualTo(1);
    }

    @Test
    void recordUseIsWhatEventuallyExhaustsTheQuota() {
        var quota = UsageQuota.of(2, 0);

        assertThat(quota.recordUse().exhausted()).isFalse();
        assertThat(quota.recordUse().recordUse().exhausted()).isTrue();
    }

    @Test
    void rejectsANegativeLimit() {
        var thrown = catchThrowable(() -> UsageQuota.of(-1, 0));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("usageLimit must be non-negative");
    }

    @Test
    void rejectsAMissingOrNegativeUsedCount() {
        assertThat(catchThrowable(() -> UsageQuota.of(100, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("usedCount cannot be null");
        assertThat(catchThrowable(() -> UsageQuota.of(100, -1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("usedCount must be non-negative");
    }
}
