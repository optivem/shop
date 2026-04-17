/**
 * Marker base type for port-layer "When" steps. Subtypes (WhenPlaceOrder,
 * WhenCancelOrder, etc.) extend this to expose a fluent chain that ends in
 * `then(): ThenResultStage` (or a use-case-specific Then stage).
 *
 * This base exists for structural parity with Java's `WhenStep`; the current
 * TypeScript port interfaces do not share a concrete contract because each
 * step exposes domain-specific methods.
 */
export type WhenStep = object;
