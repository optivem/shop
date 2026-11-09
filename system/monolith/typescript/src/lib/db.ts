import { Pool, type PoolClient } from 'pg';
import Decimal from 'decimal.js';
import { envOrDefault } from './env';
import type { OrderStatus } from './order-status';

const MONEY_SCALE = 2;
const RATE_SCALE = 4;

// Money is kept exact (Decimal) through the computation and rounded exactly once, here at the
// persistence boundary, to the column scale (NUMERIC(10,2) money, NUMERIC(5,4) rates).
function money(value: Decimal): string {
  return value.toFixed(MONEY_SCALE, Decimal.ROUND_HALF_UP);
}

function rate(value: Decimal): string {
  return value.toFixed(RATE_SCALE, Decimal.ROUND_HALF_UP);
}

const pool = new Pool({
  host: envOrDefault('POSTGRES_DB_HOST', 'localhost'),
  port: Number.parseInt(envOrDefault('POSTGRES_DB_PORT', '5432'), 10),
  database: envOrDefault('POSTGRES_DB_NAME', 'app'),
  user: envOrDefault('POSTGRES_DB_USER', 'app'),
  password: envOrDefault('POSTGRES_DB_PASSWORD', 'app'),
});

// Either the pool or a client checked out for a transaction.
export type Queryable = Pool | PoolClient;

export async function inTransaction<T>(work: (db: Queryable) => Promise<T>): Promise<T> {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await work(client);
    await client.query('COMMIT');
    return result;
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
}

/** orders.shipping_address (JSONB): the address an order ships to. */
export interface ShippingAddress {
  firstName: string;
  lastName: string;
  street: string;
  postalCode: string;
  city: string;
  country: string;
}

export interface OrderRow {
  id: number;
  order_number: string;
  order_timestamp: Date;
  country: string;
  sku: string;
  quantity: number;
  unit_price: string;
  base_price: string;
  discount_rate: string;
  discount_amount: string;
  subtotal_price: string;
  tax_rate: string;
  tax_amount: string;
  total_price: string;
  applied_coupon_code: string | null;
  status: OrderStatus;
  /** Token subject of the customer who placed the order; null for orders that predate ownership. */
  owner: string | null;
  owner_name: string | null;
  /** Null for Quick orders and orders that predate addresses; node-postgres returns JSONB parsed. */
  shipping_address: ShippingAddress | null;
  /** Optimistic-locking counter; BIGINT, so node-postgres returns it as a string. */
  version: string;
}

export interface CouponRow {
  id: number;
  code: string;
  discount_rate: string;
  valid_from: Date | null;
  valid_to: Date | null;
  usage_limit: number | null;
  used_count: number;
}

export async function insertOrder(order: {
  orderNumber: string;
  orderTimestamp: Date;
  country: string;
  sku: string;
  quantity: number;
  unitPrice: Decimal;
  basePrice: Decimal;
  discountRate: Decimal;
  discountAmount: Decimal;
  subtotalPrice: Decimal;
  taxRate: Decimal;
  taxAmount: Decimal;
  totalPrice: Decimal;
  appliedCouponCode: string | null;
  status: OrderStatus;
  owner: string;
  ownerName: string;
  shippingAddress?: ShippingAddress | null;
}, db: Queryable = pool): Promise<void> {
  await db.query(
    `INSERT INTO orders (order_number, order_timestamp, country, sku, quantity, unit_price, base_price, discount_rate, discount_amount, subtotal_price, tax_rate, tax_amount, total_price, applied_coupon_code, status, owner, owner_name, shipping_address)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18)`,
    [
      order.orderNumber, order.orderTimestamp, order.country, order.sku, order.quantity,
      money(order.unitPrice), money(order.basePrice), rate(order.discountRate), money(order.discountAmount),
      money(order.subtotalPrice), rate(order.taxRate), money(order.taxAmount), money(order.totalPrice),
      order.appliedCouponCode, order.status, order.owner, order.ownerName,
      order.shippingAddress ? JSON.stringify(order.shippingAddress) : null
    ]
  );
}

export async function findByOrderNumber(orderNumber: string): Promise<OrderRow | null> {
  const result = await pool.query<OrderRow>(
    'SELECT * FROM orders WHERE order_number = $1',
    [orderNumber]
  );
  return result.rows[0] ?? null;
}

/** One page of rows: LIMIT/OFFSET. Omitted, every matching row is read. */
export interface RowWindow {
  limit: number;
  offset: number;
}

/**
 * Which orders a history lists: `orderNumber` matches as a case-insensitive substring; `owner` (token subject)
 * restricts to one customer's orders, null or absent (admin) lists every order; `ownerName` matches the
 * username exactly (the admin customer filter).
 */
export interface OrderFilter {
  orderNumber?: string;
  owner?: string | null;
  ownerName?: string;
}

function orderConditions(filter: OrderFilter) {
  const conditions: string[] = [];
  const params: (string | number)[] = [];
  if (filter.orderNumber) {
    params.push(`%${filter.orderNumber}%`);
    conditions.push(`LOWER(order_number) LIKE LOWER($${params.length})`);
  }
  if (filter.owner !== null && filter.owner !== undefined) {
    params.push(filter.owner);
    conditions.push(`owner = $${params.length}`);
  }
  if (filter.ownerName) {
    params.push(filter.ownerName);
    conditions.push(`owner_name = $${params.length}`);
  }
  const where = conditions.length > 0 ? ` WHERE ${conditions.join(' AND ')}` : '';
  return { where, params };
}

function windowClause(params: (string | number)[], window?: RowWindow): string {
  if (!window) {
    return '';
  }
  params.push(window.limit, window.offset);
  return ` LIMIT $${params.length - 1} OFFSET $${params.length}`;
}

// Newest first, with the id as the tie-break so pages are stable.
export async function findAllOrders(filter: OrderFilter = {}, window?: RowWindow): Promise<OrderRow[]> {
  const { where, params } = orderConditions(filter);
  const result = await pool.query<OrderRow>(
    `SELECT * FROM orders${where} ORDER BY order_timestamp DESC, id DESC${windowClause(params, window)}`,
    params
  );
  return result.rows;
}

// The same predicate as findAllOrders, counted: the total a paged response reports.
export async function countOrders(filter: OrderFilter = {}): Promise<number> {
  const { where, params } = orderConditions(filter);
  const result = await pool.query<{ count: string }>(`SELECT COUNT(*) AS count FROM orders${where}`, params);
  return Number(result.rows[0]?.count ?? 0);
}

// Another customer's order is reported as missing, so its existence is not revealed. Orders without an
// owner are visible to admins only.
export async function findAccessibleOrder(
  orderNumber: string,
  caller: { subject: string; admin: boolean }
): Promise<OrderRow | null> {
  const order = await findByOrderNumber(orderNumber);
  if (!order || !(caller.admin || order.owner === caller.subject)) {
    return null;
  }
  return order;
}

// Optimistic locking: applies only if the row still has the version that was read; false means
// another update got there first and the caller should report a conflict.
export async function updateOrderStatus(order: Pick<OrderRow, 'order_number' | 'version'>, status: OrderStatus): Promise<boolean> {
  const result = await pool.query(
    'UPDATE orders SET status = $1, version = version + 1 WHERE order_number = $2 AND version = $3',
    [status, order.order_number, order.version]
  );
  return result.rowCount === 1;
}

export async function insertCoupon(coupon: {
  code: string;
  discountRate: Decimal;
  validFrom?: Date | null;
  validTo?: Date | null;
  usageLimit?: number | null;
}): Promise<void> {
  await pool.query(
    `INSERT INTO coupons (code, discount_rate, valid_from, valid_to, usage_limit, used_count)
     VALUES ($1, $2, $3, $4, $5, 0)`,
    [coupon.code, rate(coupon.discountRate), coupon.validFrom ?? null, coupon.validTo ?? null, coupon.usageLimit ?? null]
  );
}

export async function findCouponByCode(code: string): Promise<CouponRow | null> {
  const result = await pool.query<CouponRow>(
    'SELECT * FROM coupons WHERE code = $1',
    [code]
  );
  return result.rows[0] ?? null;
}

// Claims one use of the coupon; false when its usage limit is already reached. The check and the
// increment are a single statement, so concurrent claims cannot exceed the limit.
export async function tryIncrementCouponUsage(code: string, db: Queryable = pool): Promise<boolean> {
  const result = await db.query(
    'UPDATE coupons SET used_count = used_count + 1 WHERE code = $1 AND (usage_limit IS NULL OR used_count < usage_limit)',
    [code]
  );
  return result.rowCount === 1;
}

// Newest first, so a freshly published coupon is always on the first page.
export async function findAllCoupons(window?: RowWindow): Promise<CouponRow[]> {
  const params: (string | number)[] = [];
  const result = await pool.query<CouponRow>(
    `SELECT * FROM coupons ORDER BY id DESC${windowClause(params, window)}`,
    params
  );
  return result.rows;
}

export async function countCoupons(): Promise<number> {
  const result = await pool.query<{ count: string }>('SELECT COUNT(*) AS count FROM coupons');
  return Number(result.rows[0]?.count ?? 0);
}

export interface IdempotencyKeyRow {
  owner: string;
  idempotency_key: string;
  request_hash: string;
  response_status: number;
  response_body: string;
  created_at: Date;
}

const IDEMPOTENCY_KEY_UNIQUE_CONSTRAINT = 'uq_idempotency_keys_owner_key';

export async function findIdempotencyKey(owner: string, key: string, db: Queryable = pool): Promise<IdempotencyKeyRow | null> {
  const result = await db.query<IdempotencyKeyRow>(
    'SELECT owner, idempotency_key, request_hash, response_status, response_body, created_at FROM idempotency_keys WHERE owner = $1 AND idempotency_key = $2',
    [owner, key]
  );
  return result.rows[0] ?? null;
}

// Removes this key's row only if it expired (created at or before the cutoff); a live row is kept.
export async function deleteExpiredIdempotencyKey(owner: string, key: string, cutoff: Date, db: Queryable = pool): Promise<void> {
  await db.query(
    'DELETE FROM idempotency_keys WHERE owner = $1 AND idempotency_key = $2 AND created_at <= $3',
    [owner, key, cutoff]
  );
}

export async function insertIdempotencyKey(row: {
  owner: string;
  key: string;
  requestHash: string;
  responseStatus: number;
  responseBody: string;
  createdAt: Date;
}, db: Queryable = pool): Promise<void> {
  await db.query(
    `INSERT INTO idempotency_keys (owner, idempotency_key, request_hash, response_status, response_body, created_at)
     VALUES ($1, $2, $3, $4, $5, $6)`,
    [row.owner, row.key, row.requestHash, row.responseStatus, row.responseBody, row.createdAt]
  );
}

/** Unique-index violation on (owner, idempotency_key): a concurrent request with the same key won. */
export function isIdempotencyKeyConflict(error: unknown): boolean {
  const pgError = error as { code?: string; constraint?: string } | null;
  return pgError?.code === '23505' && pgError?.constraint === IDEMPOTENCY_KEY_UNIQUE_CONSTRAINT;
}

export interface CustomerProfileRow {
  owner: string;
  first_name: string;
  last_name: string;
  street: string;
  postal_code: string;
  city: string;
  country: string;
  updated_at: Date;
}

const PROFILE_COLUMNS = 'owner, first_name, last_name, street, postal_code, city, country, updated_at';

export async function findCustomerProfile(owner: string): Promise<CustomerProfileRow | null> {
  const result = await pool.query<CustomerProfileRow>(
    `SELECT ${PROFILE_COLUMNS} FROM customer_profiles WHERE owner = $1`,
    [owner]
  );
  return result.rows[0] ?? null;
}

// One profile per customer: inserts it, or replaces the existing one in the same statement.
export async function upsertCustomerProfile(profile: {
  owner: string;
  firstName: string;
  lastName: string;
  street: string;
  postalCode: string;
  city: string;
  country: string;
}): Promise<CustomerProfileRow> {
  const result = await pool.query<CustomerProfileRow>(
    `INSERT INTO customer_profiles (owner, first_name, last_name, street, postal_code, city, country, updated_at)
     VALUES ($1, $2, $3, $4, $5, $6, $7, now())
     ON CONFLICT (owner) DO UPDATE SET
       first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name, street = EXCLUDED.street,
       postal_code = EXCLUDED.postal_code, city = EXCLUDED.city, country = EXCLUDED.country, updated_at = now()
     RETURNING ${PROFILE_COLUMNS}`,
    [profile.owner, profile.firstName, profile.lastName, profile.street, profile.postalCode, profile.city, profile.country]
  );
  const row = result.rows[0];
  if (!row) {
    throw new Error('Profile upsert returned no row');
  }
  return row;
}

// Releases the connection pool. Tests must call this before stopping a
// Testcontainers Postgres, otherwise the container shutdown surfaces as an
// unhandled "terminating connection due to administrator command" pool error.
export async function closePool(): Promise<void> {
  await pool.end();
}
