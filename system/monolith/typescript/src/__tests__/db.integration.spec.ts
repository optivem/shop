import Decimal from 'decimal.js';
import { Client } from 'pg';
import {
  PostgreSqlContainer,
  StartedPostgreSqlContainer,
} from '@testcontainers/postgresql';
import * as fs from 'fs';
import * as path from 'path';

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
  let db: typeof import('../lib/db');

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
    });

    const found = await db.findByOrderNumber('ORD-100');

    expect(found).not.toBeNull();
    expect(found!.sku).toBe('BOOK-123');
    // numeric columns round-trip as strings via node-postgres.
    expect(Number(found!.total_price)).toBeCloseTo(22.0);
    expect(found!.status).toBe('PLACED');
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
});
