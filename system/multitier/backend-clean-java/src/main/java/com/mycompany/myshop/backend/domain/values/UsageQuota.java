package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

/**
 * How often something may be used and how often it has been. An absent {@code limit} means
 * unlimited — the nullable {@code usage_limit} column, given a type.
 *
 * <p>Immutable: {@link #recordUse()} returns the next quota rather than incrementing this one, so
 * "the count moved" is a visible assignment on the entity holding it rather than a side effect on a
 * shared object. Pairing the limit with the count is what lets {@link #exhausted()} answer for
 * itself; while they sat apart, the coupon's redemption and the coupon's limit check had no way to
 * refer to the same thing.
 */
public record UsageQuota(Integer limit, int used) {

    public UsageQuota {
        Guard.notNegative(limit, "usageLimit");
        if (used < 0) {
            throw new IllegalArgumentException("usedCount must be non-negative");
        }
    }

    /** Accepts the nullable {@code used} that storage and the wire hand over; {@code limit} may be null. */
    public static UsageQuota of(Integer limit, Integer used) {
        Guard.notNull(used, "usedCount");
        return new UsageQuota(limit, used);
    }

    public boolean exhausted() {
        return limit != null && used >= limit;
    }

    public UsageQuota recordUse() {
        return new UsageQuota(limit, used + 1);
    }
}
