package com.mycompany.myshop.backend.domain.rules;

// The domain's one way of saying "no, and here is why".
//
// It travels one way: THROWN, wrapped in ValidationException, and caught exactly once, in
// RefusalTranslatingUseCase. The rule that picks the transport is about the caller, not the callee
// -- not "is this method one decision or a pipeline?" but "does anyone act differently on which
// refusal came back?". In this domain almost nobody does: every refusal ends as a 422 carrying a
// field and a message. Handing it back as a value would make every frame between the rule and that
// 422 re-implement stack unwinding by hand, to arrive where a throw arrives on its own.
//
// The narrow exception is OrderNumber.parse, which returns a Result because its two callers really
// do disagree -- DeliverOrder answers a malformed number with "malformed", CancelOrder with "no such
// order". That is the bar: a branch someone actually takes, not one someone might.
//
// Being a sealed type is what makes the single catch safe. The boundary translates by switching over
// this interface, so a new kind of refusal is a compile error there rather than a string nobody
// matched. Thrown, but not untyped.
public sealed interface RuleViolation {

    // Nullable: some rules are about the request as a whole rather than one field, and the response
    // has always reported those with no field name. Every record below therefore has a second,
    // message-only constructor -- a rule with no field says so by not naming one, rather than by
    // spelling out a null at every call site.
    String field();

    String message();

    // A transition was asked for from a status that does not allow it.
    record NotInStatus(String field, String message) implements RuleViolation {

        public NotInStatus(String message) {
            this(null, message);
        }
    }

    // A quota, limit or allowance is used up.
    record LimitReached(String field, String message) implements RuleViolation {

        public LimitReached(String message) {
            this(null, message);
        }
    }

    // A value cannot be built from what was supplied.
    record Malformed(String field, String message) implements RuleViolation {

        public Malformed(String message) {
            this(null, message);
        }
    }

    // A referenced thing does not exist, as a rule rather than as a lookup miss.
    record Missing(String field, String message) implements RuleViolation {

        public Missing(String message) {
            this(null, message);
        }
    }

    // A rule that is about time or policy rather than about one thing's state.
    record NotAllowed(String field, String message) implements RuleViolation {

        public NotAllowed(String message) {
            this(null, message);
        }
    }
}
