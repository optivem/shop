// The SPA cart lives in localStorage["myshop.cart"] as [{ sku, quantity }] (storefront-ux §6.3).
// Storage can be missing or throw (private mode, quota, disabled cookies), so every access is guarded
// and the cart then simply lives in memory for this visit.

import { clampQuantity } from '../utils/quantity';
import type { CartLine } from './cart-context';

export const CART_STORAGE_KEY = 'myshop.cart';

function isCartLine(value: unknown): value is CartLine {
  if (typeof value !== 'object' || value === null) return false;
  const line = value as Record<string, unknown>;
  return typeof line.sku === 'string' && line.sku !== '' && typeof line.quantity === 'number' && Number.isInteger(line.quantity);
}

export function loadCart(storage: Storage | undefined = safeLocalStorage()): CartLine[] {
  try {
    const raw = storage?.getItem(CART_STORAGE_KEY);
    if (!raw) return [];
    const parsed: unknown = JSON.parse(raw);
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

export function saveCart(lines: CartLine[], storage: Storage | undefined = safeLocalStorage()): void {
  try {
    storage?.setItem(CART_STORAGE_KEY, JSON.stringify(lines));
  } catch {
    // Storage unavailable or full: the in-memory cart still works for this visit.
  }
}

function safeLocalStorage(): Storage | undefined {
  try {
    return globalThis.localStorage;
  } catch {
    return undefined;
  }
}
