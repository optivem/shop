import Decimal from 'decimal.js';
import { Client } from 'pg';
import {
  PostgreSqlContainer,
  type StartedPostgreSqlContainer,
} from '@testcontainers/postgresql';
import * as fs from 'fs';
import * as path from 'path';
import type * as DbModule from '../lib/db';
import type * as IdempotentOrderModule from '../lib/idempotent-order';

// Canonical schema — the same migrations Flyway applies for the Java backend
// (system/db/migrations). Applied to the throwaway container so the test
// exercises the real DDL.
const MIGRATIONS_DIR = path.resolve(__dirname, '../../../../db/migrations');

async function applyMigrations(
  container: StartedPostgreSqlContainer,
): Promise<void> {
  const client = new Client({
    host: container.getHost(),
    port: container.getPort(),
    user: container.getUsername(),
    password: container.getPassword(),
    database: container.getDatabase(),
  });
  await client.connect();
  try {
    const files = fs
      .readdirSync(MIGRATIONS_DIR)
      .filter((f) => f.endsWith('.sql'))
      .sort();
    for (const file of files) {
      const sql = fs.readFileSync(path.join(MIGRATIONS_DIR, file), 'utf8');
      await client.query(sql);
    }
  } finally {
    await client.end();
  }
}

describe('db adapter [integration]', () => {
  let postgres: StartedPostgreSqlContainer;
  let db: typeof DbModule;
  let idempotent: typeof IdempotentOrderModule;

  beforeAll(async () => {
    postgres = await new PostgreSqlContainer('postgres:16-alpine')
      .withDatabase('app')
      .withUsername('app')
      .withPassword('app')
      .start();

    await applyMigrations(postgres);

    // Set env BEFORE importing db.ts: its pg Pool is built at module-load time
    // from these vars, so the dynamic import must come after they are set.
    process.env.POSTGRES_DB_HOST = postgres.getHost();
    process.env.POSTGRES_DB_PORT = String(postgres.getPort());
    process.env.POSTGRES_DB_NAME = postgres.getDatabase();
    process.env.POSTGRES_DB_USER = postgres.getUsername();
    process.env.POSTGRES_DB_PASSWORD = postgres.getPassword();

    db = await import('../lib/db');
    idempotent = await import('../lib/idempotent-order');
  }, 120_000);

  afterAll(async () => {
    // Close the pg pool before stopping the container, else the shutdown
    // surfaces as an unhandled "terminating connection due to administrator
    // command" error and fails the suite.
    if (db) {
      await db.closePool();
    }
    if (postgres) {
      await postgres.stop();
    }
  }, 60_000);

  it('inserts an order and finds it by order number', async () => {
    await db.insertOrder({
      orderNumber: 'ORD-100',
      orderTimestamp: new Date('2026-01-01T00:00:00Z'),
      country: 'US',
      sku: 'BOOK-123',
      quantity: 2,
      unitPrice: new Decimal('10.0'),
      basePrice: new Decimal('20.0'),
      discountRate: new Decimal('0'),
      discountAmount: new Decimal('0'),
      subtotalPrice: new Decimal('20.0'),
      taxRate: new Decimal('0.1'),
      taxAmount: new Decimal('2.0'),
      totalPrice: new Decimal('22.0'),
      appliedCouponCode: null,
      status: 'PLACED',
      owner: 'sub-a',
      ownerName: 'customer-a',
    });

    const found = await db.findByOrderNumber('ORD-100');

    expect(found).toMatchObject({ sku: 'BOOK-123', status: 'PLACED', owner: 'sub-a', owner_name: 'customer-a' });
    // numeric columns round-trip as strings via node-postgres.
    expect(Number(found?.total_price)).toBeCloseTo(22.0);
  });

  it('lists a customer only their own orders and an admin all of them', async () => {
    const order = (orderNumber: string, owner: string, ownerName: string) => ({
      orderNumber,
      orderTimestamp: new Date('2026-01-02T00:00:00Z'),
      country: 'US',
      sku: 'BOOK-123',
      quantity: 1,
      unitPrice: new Decimal('10.0'),
      basePrice: new Decimal('10.0'),
      discountRate: new Decimal('0'),
      discountAmount: new Decimal('0'),
      subtotalPrice: new Decimal('10.0'),
      taxRate: new Decimal('0.1'),
      taxAmount: new Decimal('1.0'),
      totalPrice: new Decimal('11.0'),
      appliedCouponCode: null,
      status: 'PLACED' as const,
      owner,
      ownerName,
    });
    await db.insertOrder(order('ORD-OWN-A', 'sub-a', 'customer-a'));
    await db.insertOrder(order('ORD-OWN-B', 'sub-b', 'customer-b'));

    const mine = (await db.findAllOrders({ owner: 'sub-a' })).map((o) => o.order_number);
    const filtered = (await db.findAllOrders({ orderNumber: 'own-b', owner: 'sub-a' })).map((o) => o.order_number);
    const all = (await db.findAllOrders({ orderNumber: 'ORD-OWN' })).map((o) => o.order_number);

    expect(mine).toContain('ORD-OWN-A');
    expect(mine).not.toContain('ORD-OWN-B');
    expect(filtered).toEqual([]);
    expect(all).toEqual(expect.arrayContaining(['ORD-OWN-A', 'ORD-OWN-B']));

    const chosen = (await db.findAllOrders({ orderNumber: 'ORD-OWN', ownerName: 'customer-b' })).map(
      (o) => o.order_number
    );
    expect(chosen).toEqual(['ORD-OWN-B']);
    expect(await db.countOrders({ orderNumber: 'ORD-OWN', ownerName: 'customer-b' })).toBe(1);

    const admin = { subject: 'sub-admin', admin: true };
    const customerA = { subject: 'sub-a', admin: false };
    expect(await db.findAccessibleOrder('ORD-OWN-B', customerA)).toBeNull();
    expect(await db.findAccessibleOrder('ORD-OWN-A', customerA)).not.toBeNull();
    expect(await db.findAccessibleOrder('ORD-OWN-B', admin)).not.toBeNull();
  });

  it('rejects a status update made from a stale read', async () => {
    const order = await db.findByOrderNumber('ORD-100');
    if (!order) throw new Error('seed order missing');

    expect(await db.updateOrderStatus(order, 'DELIVERED')).toBe(true);
    expect(await db.updateOrderStatus(order, 'CANCELLED')).toBe(false);
    expect((await db.findByOrderNumber('ORD-100'))?.status).toBe('DELIVERED');
  });

  it('never lets concurrent claims exceed a coupon usage limit', async () => {
    await db.insertCoupon({ code: 'RACE-2', discountRate: new Decimal('0.1'), usageLimit: 2 });

    const claims = await Promise.all(
      Array.from({ length: 10 }, () => db.tryIncrementCouponUsage('RACE-2')),
    );

    expect(claims.filter(Boolean)).toHaveLength(2);
    expect((await db.findCouponByCode('RACE-2'))?.used_count).toBe(2);
  });

  it('rolls back a coupon claim when the transaction fails', async () => {
    await db.insertCoupon({ code: 'ROLLBACK-1', discountRate: new Decimal('0.1'), usageLimit: 1 });

    await expect(
      db.inTransaction(async (tx) => {
        await db.tryIncrementCouponUsage('ROLLBACK-1', tx);
        throw new Error('insert failed');
      }),
    ).rejects.toThrow('insert failed');

    expect((await db.findCouponByCode('ROLLBACK-1'))?.used_count).toBe(0);
  });

  const pagedOrder = (orderNumber: string, orderTimestamp: Date, owner = 'sub-page') => ({
    orderNumber,
    orderTimestamp,
    country: 'US',
    sku: 'BOOK-123',
    quantity: 1,
    unitPrice: new Decimal('10.0'),
    basePrice: new Decimal('10.0'),
    discountRate: new Decimal('0'),
    discountAmount: new Decimal('0'),
    subtotalPrice: new Decimal('10.0'),
    taxRate: new Decimal('0.1'),
    taxAmount: new Decimal('1.0'),
    totalPrice: new Decimal('11.0'),
    appliedCouponCode: null,
    status: 'PLACED' as const,
    owner,
    ownerName: owner,
  });

  it('pages orders newest first with an id tie-break, stable across pages', async () => {
    const base = Date.parse('2027-01-01T00:00:00Z');
    for (let i = 0; i < 5; i++) {
      // The last two share a timestamp, so the id decides their order.
      await db.insertOrder(pagedOrder(`ORD-PAGE-${i}`, new Date(base + Math.min(i, 3) * 60_000)));
    }

    const pages = await Promise.all(
      [0, 2, 4].map((offset) => db.findAllOrders({ owner: 'sub-page' }, { limit: 2, offset }))
    );
    const pastEnd = await db.findAllOrders({ owner: 'sub-page' }, { limit: 2, offset: 6 });

    expect(pages.flat().map((o) => o.order_number)).toEqual([
      'ORD-PAGE-4', 'ORD-PAGE-3', 'ORD-PAGE-2', 'ORD-PAGE-1', 'ORD-PAGE-0',
    ]);
    expect(await db.countOrders({ owner: 'sub-page' })).toBe(5);
    expect(await db.countOrders({ orderNumber: 'page-4', owner: 'sub-page' })).toBe(1);
    expect(pastEnd).toEqual([]);
  });

  it('stores the shipping address as JSONB and reads it back parsed', async () => {
    const address = {
      firstName: 'Ana', lastName: 'Novak', street: 'Main St 1', postalCode: '10115', city: 'Berlin', country: 'DE',
    };
    await db.insertOrder({ ...pagedOrder('ORD-ADDR-1', new Date('2027-03-01T00:00:00Z'), 'sub-addr'), shippingAddress: address });
    await db.insertOrder(pagedOrder('ORD-ADDR-2', new Date('2027-03-01T00:00:00Z'), 'sub-addr'));

    expect((await db.findByOrderNumber('ORD-ADDR-1'))?.shipping_address).toEqual(address);
    expect((await db.findByOrderNumber('ORD-ADDR-2'))?.shipping_address).toBeNull();
  });

  it('upserts one profile per owner', async () => {
    const profile = {
      owner: 'sub-profile', firstName: 'Ana', lastName: 'Novak', street: 'Main St 1', postalCode: '10115',
      city: 'Berlin', country: 'DE',
    };
    expect(await db.findCustomerProfile('sub-profile')).toBeNull();

    await db.upsertCustomerProfile(profile);
    const updated = await db.upsertCustomerProfile({ ...profile, city: 'London', country: 'GB' });

    expect(updated).toMatchObject({ owner: 'sub-profile', city: 'London', country: 'GB' });
    expect(await db.findCustomerProfile('sub-profile')).toMatchObject({ first_name: 'Ana', city: 'London' });
  });

  it('pages coupons newest first', async () => {
    for (const code of ['PAGE-C-1', 'PAGE-C-2', 'PAGE-C-3']) {
      await db.insertCoupon({ code, discountRate: new Decimal('0.1') });
    }

    const firstTwo = (await db.findAllCoupons({ limit: 2, offset: 0 })).map((c) => c.code);

    expect(firstTwo).toEqual(['PAGE-C-3', 'PAGE-C-2']);
    expect(await db.countCoupons()).toBeGreaterThanOrEqual(3);
  });

  it('concurrent requests with the same key place exactly one order and use the coupon once', async () => {
    await db.insertCoupon({ code: 'RACE-KEY', discountRate: new Decimal('0.1'), usageLimit: 5 });
    const scope = { owner: 'sub-race', key: 'race-key', fingerprint: 'fp-1' };

    const outcomes = await Promise.all(
      Array.from({ length: 8 }, (_, i) => {
        const orderNumber = `ORD-RACE-${i}`;
        return idempotent.placeIdempotently(
          scope,
          { status: 201, body: JSON.stringify({ orderNumber }) },
          async (tx) => {
            if (!(await db.tryIncrementCouponUsage('RACE-KEY', tx))) throw new Error('limit');
            await db.insertOrder(pagedOrder(orderNumber, new Date('2027-02-01T00:00:00Z'), 'sub-race'), tx);
          }
        );
      })
    );

    expect(outcomes.filter((o) => o.kind === 'placed')).toHaveLength(1);
    expect(outcomes.filter((o) => o.kind === 'replay')).toHaveLength(7);
    expect(await db.countOrders({ orderNumber: 'ORD-RACE' })).toBe(1);
    expect((await db.findCouponByCode('RACE-KEY'))?.used_count).toBe(1);
    const stored = await db.findIdempotencyKey('sub-race', 'race-key');
    const placedNumber = (JSON.parse(stored?.response_body ?? '{}') as { orderNumber: string }).orderNumber;
    expect(await db.findByOrderNumber(placedNumber)).not.toBeNull();
  });

  it('answers a replay from the stored row and rejects a different request with the same key', async () => {
    const scope = { owner: 'sub-replay', key: 'key-1', fingerprint: 'fp-a' };
    await idempotent.placeIdempotently(scope, { status: 201, body: '{"orderNumber":"ORD-R-1"}' }, async (tx) => {
      await db.insertOrder(pagedOrder('ORD-R-1', new Date('2027-03-01T00:00:00Z'), 'sub-replay'), tx);
    });

    expect(await idempotent.findStoredAnswer(scope)).toEqual({ kind: 'replay', status: 201, body: '{"orderNumber":"ORD-R-1"}' });
    expect(await idempotent.findStoredAnswer({ ...scope, fingerprint: 'fp-b' })).toEqual({ kind: 'conflict' });
    expect(await idempotent.findStoredAnswer({ ...scope, owner: 'sub-other' })).toBeNull();
  });

  it('leaves the key free when the request fails, and replaces an expired key', async () => {
    const scope = { owner: 'sub-fail', key: 'key-1', fingerprint: 'fp-a' };
    await expect(
      idempotent.placeIdempotently(scope, { status: 201, body: '{}' }, () =>
        Promise.reject(new Error('insert failed'))
      )
    ).rejects.toThrow('insert failed');
    expect(await db.findIdempotencyKey('sub-fail', 'key-1')).toBeNull();

    await db.insertIdempotencyKey({
      owner: 'sub-fail', key: 'old', requestHash: 'stale', responseStatus: 201,
      responseBody: '{"orderNumber":"ORD-OLD"}', createdAt: new Date(Date.now() - 25 * 60 * 60 * 1000),
    });
    const expiredScope = { owner: 'sub-fail', key: 'old', fingerprint: 'fp-new' };
    expect(await idempotent.findStoredAnswer(expiredScope)).toBeNull();
    const outcome = await idempotent.placeIdempotently(
      expiredScope, { status: 201, body: '{"orderNumber":"ORD-NEW"}' }, () => Promise.resolve()
    );
    expect(outcome).toEqual({ kind: 'placed' });
    expect((await db.findIdempotencyKey('sub-fail', 'old'))?.request_hash).toBe('fp-new');
  });
});
