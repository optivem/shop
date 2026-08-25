package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.rules.RuleViolation;

// Cancellation is the one transition whose branches a caller can act on differently: an order that
// was already cancelled is a retry arriving twice, while an order in any other status is a genuine
// rejection. A Result flattens both into "error, with a message" and leaves the use case to tell
// them apart by wording. A sealed outcome puts the difference in front of the compiler instead.
//
// Both refusals still carry a RuleViolation, so the wording stays in the domain that owns the rule.
// Today the use case answers both with an error, which is what the acceptance suite pins; treating
// AlreadyCancelled as an idempotent success is now a one-line change in CancelOrder rather than a
// redesign, which is the point of naming the branch at all.
public sealed interface CancelOutcome {

    record Cancelled() implements CancelOutcome { }

    record AlreadyCancelled(RuleViolation violation) implements CancelOutcome { }

    record NotCancellable(RuleViolation violation) implements CancelOutcome { }
}
