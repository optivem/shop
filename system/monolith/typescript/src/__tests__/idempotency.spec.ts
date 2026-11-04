import { isExpired, requestFingerprint, resolveIdempotencyKey } from '../lib/idempotency';

const order = { sku: 'BOOK-123', quantity: 2, country: 'US' };

describe('resolveIdempotencyKey', () => {
  it.each([null, undefined, '', '   '])('treats a missing or blank key (%p) as absent', (key) => {
    expect(resolveIdempotencyKey(key)).toEqual({ ok: true, key: null });
  });

  it('keeps a key of up to 255 characters as sent', () => {
    expect(resolveIdempotencyKey('key-1')).toEqual({ ok: true, key: 'key-1' });
    expect(resolveIdempotencyKey('k'.repeat(255)).ok).toBe(true);
  });

  it('rejects a longer key on the header field', () => {
    expect(resolveIdempotencyKey('k'.repeat(256))).toEqual({
      ok: false,
      errors: [{ field: 'Idempotency-Key', message: 'Idempotency-Key must be at most 255 characters' }],
    });
  });
});

describe('requestFingerprint', () => {
  it('is a stable SHA-256 hex over the request fields', () => {
    expect(requestFingerprint(order)).toMatch(/^[0-9a-f]{64}$/);
    expect(requestFingerprint(order)).toBe(requestFingerprint({ ...order, couponCode: null }));
    expect(requestFingerprint({ ...order, couponCode: 'SAVE10' })).not.toBe(requestFingerprint(order));
    expect(requestFingerprint({ ...order, quantity: 3 })).not.toBe(requestFingerprint(order));
  });
});

describe('isExpired', () => {
  it('expires a key 24 hours after first use', () => {
    const createdAt = new Date('2026-01-01T00:00:00Z');
    expect(isExpired(createdAt, new Date('2026-01-01T23:59:59Z'))).toBe(false);
    expect(isExpired(createdAt, new Date('2026-01-02T00:00:00Z'))).toBe(true);
  });
});
