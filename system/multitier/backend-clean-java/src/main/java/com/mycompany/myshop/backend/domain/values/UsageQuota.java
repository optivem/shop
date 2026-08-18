package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.domain.Guard;

public record UsageQuota(Integer limit, int used) {

    public UsageQuota {
        Guard.notNegative(limit, "usageLimit");
        if (used < 0) {
            throw new IllegalArgumentException("usedCount must be non-negative");
        }
    }

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
