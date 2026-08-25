package com.mycompany.myshop.backend.domain.rules;

// The domain's one way of saying "no, and here is why". It travels by two transports and the choice
// is made by shape, not by taste: a method that makes a single decision RETURNS it, so the caller
// cannot fail to see it; a method that is a pipeline of several fallible steps THROWS it wrapped in
// ValidationException, because a return value would turn the pipeline into a staircase of unwrapping.
// Either way the vocabulary is the same, so the use case layer has exactly one translation to write.
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
