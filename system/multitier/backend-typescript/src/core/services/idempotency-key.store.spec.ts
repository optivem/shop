import { QueryFailedError } from 'typeorm';
import { IdempotencyKeyStore } from './idempotency-key.store';
import { IdempotencyKey } from '../entities/idempotency-key.entity';
import { PlaceOrderRequest } from '../dtos/place-order-request.dto';

function request(couponCode?: string): PlaceOrderRequest {
  const r = new PlaceOrderRequest();
  r.sku = 'BOOK-123';
  r.quantity = 2;
  r.country = 'US';
  r.couponCode = couponCode;
  return r;
}

describe('IdempotencyKeyStore', () => {
  it('fingerprints sku, quantity, country and coupon code, joined by newlines', () => {
    expect(IdempotencyKeyStore.fingerprint(request())).toMatch(
      /^[0-9a-f]{64}$/,
    );
    expect(IdempotencyKeyStore.fingerprint(request())).toBe(
      IdempotencyKeyStore.fingerprint(request()),
    );
    expect(IdempotencyKeyStore.fingerprint(request('SAVE10'))).not.toBe(
      IdempotencyKeyStore.fingerprint(request()),
    );
  });

  it('treats a blank key as absent and keeps a non-blank one as sent', () => {
    expect(IdempotencyKeyStore.normalize(undefined)).toBeNull();
    expect(IdempotencyKeyStore.normalize('  ')).toBeNull();
    expect(IdempotencyKeyStore.normalize('key-1')).toBe('key-1');
    expect(IdempotencyKeyStore.normalize('k'.repeat(255))).toHaveLength(255);
    expect(() => IdempotencyKeyStore.normalize('k'.repeat(256))).toThrow(
      'Idempotency-Key must be at most 255 characters',
    );
  });

  it('expires a row 24 hours after first use', () => {
    const row = new IdempotencyKey();
    row.createdAt = new Date('2026-01-01T00:00:00Z');

    expect(
      IdempotencyKeyStore.isExpired(row, new Date('2026-01-01T23:59:59Z')),
    ).toBe(false);
    expect(
      IdempotencyKeyStore.isExpired(row, new Date('2026-01-02T00:00:00Z')),
    ).toBe(true);
  });

  it('recognises only a unique violation on the key constraint as a key conflict', () => {
    const violation = (constraint: string) =>
      new QueryFailedError('INSERT', [], {
        code: '23505',
        constraint,
      } as unknown as Error);

    expect(
      IdempotencyKeyStore.isKeyConflict(
        violation('uq_idempotency_keys_owner_key'),
      ),
    ).toBe(true);
    expect(
      IdempotencyKeyStore.isKeyConflict(violation('orders_order_number_key')),
    ).toBe(false);
    expect(IdempotencyKeyStore.isKeyConflict(new Error('boom'))).toBe(false);
  });
});
