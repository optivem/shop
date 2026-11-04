import crypto from 'node:crypto';
import type { FieldError } from './errors';

// Idempotent POST /api/orders: keys are scoped per caller and live 24 hours from first use.
export const IDEMPOTENCY_KEY_HEADER = 'Idempotency-Key';
export const MAX_IDEMPOTENCY_KEY_LENGTH = 255;
export const IDEMPOTENCY_KEY_TTL_MS = 24 * 60 * 60 * 1000;
export const IDEMPOTENCY_KEY_TOO_LONG = `Idempotency-Key must be at most ${MAX_IDEMPOTENCY_KEY_LENGTH} characters`;
export const IDEMPOTENCY_KEY_REUSED = 'Idempotency-Key has already been used with a different request';

export type KeyResolution = { ok: true; key: string | null } | { ok: false; errors: FieldError[] };

/** A blank header is the same as no header; an over-long one is a field validation error. */
export function resolveIdempotencyKey(rawKey: string | null | undefined): KeyResolution {
  if (rawKey === null || rawKey === undefined || rawKey.trim() === '') {
    return { ok: true, key: null };
  }
  if (rawKey.length > MAX_IDEMPOTENCY_KEY_LENGTH) {
    return { ok: false, errors: [{ field: IDEMPOTENCY_KEY_HEADER, message: IDEMPOTENCY_KEY_TOO_LONG }] };
  }
  return { ok: true, key: rawKey };
}

/** SHA-256 (hex) over sku, quantity, country and coupon code as bound, null as empty, joined by \n. */
export function requestFingerprint(request: {
  sku: string;
  quantity: number;
  country: string;
  couponCode?: string | null;
}): string {
  const parts = [request.sku, request.quantity, request.country, request.couponCode].map((value) =>
    value === undefined || value === null ? '' : String(value)
  );
  return crypto.createHash('sha256').update(parts.join('\n')).digest('hex');
}

/** Rows created at or before this instant are expired. */
export function expiryCutoff(now: Date): Date {
  return new Date(now.getTime() - IDEMPOTENCY_KEY_TTL_MS);
}

export function isExpired(createdAt: Date, now: Date): boolean {
  return new Date(createdAt).getTime() <= expiryCutoff(now).getTime();
}
