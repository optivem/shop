package com.mycompany.myshop.backend.domain.values;

import java.time.Instant;

/**
 * The window in which something may be used. Either bound may be absent, meaning open-ended in that
 * direction — the two nullable {@code valid_from} / {@code valid_to} columns, given a type.
 *
 * <p>Both bounds are inclusive. The pair is a type rather than two fields on
 * {@link com.mycompany.myshop.backend.domain.entities.Coupon} because they are never independent:
 * the ordering rule couples them, and asking "is this usable now?" reads both. Holding them together
 * puts that rule beside the data it constrains instead of in the constructor of whatever happens to
 * carry the pair.
 */
public record ValidityPeriod(Instant validFrom, Instant validTo) {

    /** Open at both ends — usable at any instant. */
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
