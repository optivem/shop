// The Next.js cart lives in the "myshop_cart" cookie as JSON [{ sku, quantity }] (storefront-ux §6.3).
// The server reads it to render the cart and the header badge; the browser writes it on every change.
// The cookie is independent of the login session, so the cart survives signing in.

import { clampQuantity } from './quantity';
import type { CartLine } from './types';

export const CART_COOKIE = 'myshop_cart';
const CART_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

function isCartLine(value: unknown): value is CartLine {
  if (typeof value !== 'object' || value === null) return false;
  const line = value as Record<string, unknown>;
  return typeof line.sku === 'string' && line.sku !== '' && typeof line.quantity === 'number' && Number.isInteger(line.quantity);
}

/** The cart in a cookie value (as the server or document.cookie gives it); anything malformed is an empty cart. */
export function parseCart(raw: string | undefined | null): CartLine[] {
  if (!raw) return [];
  try {
    let json = raw;
    try {
      json = decodeURIComponent(raw);
    } catch {
      // Not URI-encoded: parse as it is.
    }
    const parsed: unknown = JSON.parse(json);
    if (!Array.isArray(parsed)) return [];
    const lines: CartLine[] = [];
    for (const item of parsed.filter(isCartLine)) {
      if (!lines.some((line) => line.sku === item.sku)) {
        lines.push({ sku: item.sku, quantity: clampQuantity(item.quantity) });
      }
    }
    return lines;
  } catch {
    return [];
  }
}

export function serializeCart(lines: CartLine[]): string {
  return encodeURIComponent(JSON.stringify(lines.map(({ sku, quantity }) => ({ sku, quantity }))));
}

/** Browser only: writes the cart cookie. Cookies can be disabled, so the in-memory cart then lives on alone. */
export function saveCartCookie(lines: CartLine[]): void {
  try {
    document.cookie = `${CART_COOKIE}=${serializeCart(lines)}; path=/; max-age=${CART_MAX_AGE_SECONDS}; samesite=lax`;
  } catch {
    // Cookies unavailable: the in-memory cart still works for this visit.
  }
}
