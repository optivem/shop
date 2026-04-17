/**
 * Marker base type for port-layer "Given" steps. Subtypes (GivenClock,
 * GivenCountry, etc.) extend this. Kept as a structural parity anchor with
 * Java's `GivenStep` — the TS port interfaces do not share a concrete
 * contract because each step exposes domain-specific methods.
 */
export type GivenStep = object;
