package com.mycompany.myshop.backend.domain.values;

import java.time.Instant;

public record ValidityPeriod(Instant validFrom, Instant validTo) {

    public static final ValidityPeriod ALWAYS = new ValidityPeriod(null, null);

    public ValidityPeriod {
        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }

    public boolean notYetValidAt(Instant at) {
        return validFrom != null && at.isBefore(validFrom);
    }

    public boolean expiredAt(Instant at) {
        return validTo != null && at.isAfter(validTo);
    }
}
