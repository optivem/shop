/**
 * Marker base type for port-layer "Then" steps. Subtypes (ThenOrder,
 * ThenCoupon, ThenSuccess, ThenFailure, etc.) extend this. Kept as a
 * structural parity anchor with Java's `ThenStep` — the TS port interfaces
 * do not share a concrete contract because each step exposes domain-specific
 * assertions.
 */
export type ThenStep = object;
