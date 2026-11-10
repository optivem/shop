// Hands the checkout result to the confirmation page. Router state carries it; sessionStorage keeps it for a
// reload of the confirmation page. Storage may be unavailable, so both calls are guarded.

import type { CheckoutResult } from './place-checkout-orders';

const KEY = 'myshop.lastCheckout';

export function saveConfirmation(result: CheckoutResult): void {
  try {
    globalThis.sessionStorage.setItem(KEY, JSON.stringify(result));
  } catch {
    // Without storage the confirmation still shows from router state.
  }
}

function isCheckoutResult(value: unknown): value is CheckoutResult {
  if (typeof value !== 'object' || value === null) return false;
  const result = value as Partial<CheckoutResult>;
  return Array.isArray(result.placed) && Array.isArray(result.rejected);
}

export function loadConfirmation(state: unknown): CheckoutResult | undefined {
  if (isCheckoutResult(state)) return state;
  try {
    const raw = globalThis.sessionStorage.getItem(KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : undefined;
    return isCheckoutResult(parsed) ? parsed : undefined;
  } catch {
    return undefined;
  }
}
