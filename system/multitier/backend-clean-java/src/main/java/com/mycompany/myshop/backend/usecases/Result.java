package com.mycompany.myshop.backend.usecases;

/**
 * Either a value or an error, never both. This is the Result (or Either) pattern, also known as
 * Railway-Oriented Programming: it lets a use case <em>declare</em> the outcomes it can produce
 * instead of throwing them past its own signature.
 *
 * <p>Sealed with exactly two cases, so a {@code switch} over a {@code Result} is exhaustive without
 * a {@code default} branch.
 *
 * <p>The rule for what belongs here: <strong>expected outcomes only</strong> — order not found,
 * order already cancelled, coupon code taken. A database that is down, a gateway that failed, a bug
 * — those stay exceptions and travel to the top. Put everything in here and every caller has to
 * check every branch, which is checked exceptions again, with the same fatigue that killed them.
 *
 * @param <T> what the use case produces when it succeeds
 * @param <E> what it reports when it does not
 */
public sealed interface Result<T, E> {

    /** True for {@link Ok}, false for {@link Err}. */
    boolean isOk();

    /** The value. Throws on an {@link Err} — check {@link #isOk()} or switch over the result. */
    T value();

    /** The error. Throws on an {@link Ok} — check {@link #isOk()} or switch over the result. */
    E error();

    record Ok<T, E>(T value) implements Result<T, E> {

        @Override
        public boolean isOk() {
            return true;
        }

        @Override
        public E error() {
            throw new IllegalStateException("A successful Result has no error");
        }
    }

    record Err<T, E>(E error) implements Result<T, E> {

        @Override
        public boolean isOk() {
            return false;
        }

        @Override
        public T value() {
            throw new IllegalStateException("A failed Result has no value");
        }
    }

    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }
}
