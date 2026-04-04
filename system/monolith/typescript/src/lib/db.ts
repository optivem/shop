import { Pool } from 'pg';

const pool = new Pool({
  host: process.env.POSTGRES_DB_HOST || 'localhost',
  port: Number.parseInt(process.env.POSTGRES_DB_PORT || '5432', 10),
  database: process.env.POSTGRES_DB_NAME || 'app',
  user: process.env.POSTGRES_DB_USER || 'app',
  password: process.env.POSTGRES_DB_PASSWORD || 'app',
});

let schemaPromise: Promise<void> | null = null;

function ensureSchema(): Promise<void> {
  schemaPromise ??= pool.query(`
    CREATE TABLE IF NOT EXISTS coupons (
      id BIGSERIAL PRIMARY KEY,
      code VARCHAR(255) NOT NULL UNIQUE,
      discount_rate NUMERIC(10,4) NOT NULL,
      valid_from TIMESTAMPTZ,
      valid_to TIMESTAMPTZ,
      usage_limit INTEGER,
      used_count INTEGER NOT NULL DEFAULT 0
    );
    CREATE TABLE IF NOT EXISTS orders (
      id BIGSERIAL PRIMARY KEY,
      order_number VARCHAR(255) NOT NULL UNIQUE,
      order_timestamp TIMESTAMPTZ NOT NULL,
      country VARCHAR(255) NOT NULL DEFAULT 'US',
      sku VARCHAR(255) NOT NULL,
      quantity INTEGER NOT NULL,
      unit_price NUMERIC(10,2) NOT NULL,
      base_price NUMERIC(10,2) NOT NULL DEFAULT 0,
      discount_rate NUMERIC(10,4) NOT NULL DEFAULT 0,
      discount_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
      subtotal_price NUMERIC(10,2) NOT NULL DEFAULT 0,
      tax_rate NUMERIC(10,4) NOT NULL DEFAULT 0,
      tax_amount NUMERIC(10,2) NOT NULL DEFAULT 0,
      total_price NUMERIC(10,2) NOT NULL,
      applied_coupon_code VARCHAR(255),
      status VARCHAR(50) NOT NULL
    )
  `).then(() => {});
  return schemaPromise;
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
  status: string;
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
  unitPrice: number;
  basePrice: number;
  discountRate: number;
  discountAmount: number;
  subtotalPrice: number;
  taxRate: number;
  taxAmount: number;
  totalPrice: number;
  appliedCouponCode: string | null;
  status: string;
}): Promise<void> {
  await ensureSchema();
  await pool.query(
    `INSERT INTO orders (order_number, order_timestamp, country, sku, quantity, unit_price, base_price, discount_rate, discount_amount, subtotal_price, tax_rate, tax_amount, total_price, applied_coupon_code, status)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)`,
    [
      order.orderNumber, order.orderTimestamp, order.country, order.sku, order.quantity,
      order.unitPrice, order.basePrice, order.discountRate, order.discountAmount,
      order.subtotalPrice, order.taxRate, order.taxAmount, order.totalPrice,
      order.appliedCouponCode, order.status
    ]
  );
}

export async function findByOrderNumber(orderNumber: string): Promise<OrderRow | null> {
  await ensureSchema();
  const result = await pool.query<OrderRow>(
    'SELECT * FROM orders WHERE order_number = $1',
    [orderNumber]
  );
  return result.rows[0] || null;
}

export async function findAllOrders(orderNumberFilter?: string): Promise<OrderRow[]> {
  await ensureSchema();
  if (orderNumberFilter) {
    const result = await pool.query<OrderRow>(
      'SELECT * FROM orders WHERE LOWER(order_number) LIKE LOWER($1) ORDER BY order_timestamp DESC',
      [`%${orderNumberFilter}%`]
    );
    return result.rows;
  }
  const result = await pool.query<OrderRow>(
    'SELECT * FROM orders ORDER BY order_timestamp DESC'
  );
  return result.rows;
}

export async function insertCoupon(coupon: {
  code: string;
  discountRate: number;
  validFrom?: Date | null;
  validTo?: Date | null;
  usageLimit?: number | null;
}): Promise<void> {
  await ensureSchema();
  await pool.query(
    `INSERT INTO coupons (code, discount_rate, valid_from, valid_to, usage_limit)
     VALUES ($1, $2, $3, $4, $5)`,
    [coupon.code, coupon.discountRate, coupon.validFrom ?? null, coupon.validTo ?? null, coupon.usageLimit ?? null]
  );
}

export async function findCouponByCode(code: string): Promise<CouponRow | null> {
  await ensureSchema();
  const result = await pool.query<CouponRow>(
    'SELECT * FROM coupons WHERE code = $1',
    [code]
  );
  return result.rows[0] || null;
}

export async function incrementCouponUsage(code: string): Promise<void> {
  await ensureSchema();
  await pool.query(
    'UPDATE coupons SET used_count = used_count + 1 WHERE code = $1',
    [code]
  );
}

export async function findAllCoupons(): Promise<CouponRow[]> {
  await ensureSchema();
  const result = await pool.query<CouponRow>('SELECT * FROM coupons ORDER BY id');
  return result.rows;
}
