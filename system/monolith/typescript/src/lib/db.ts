import { Pool } from 'pg';
import { envOrDefault } from './env';

const pool = new Pool({
  host: envOrDefault('POSTGRES_DB_HOST', 'localhost'),
  port: Number.parseInt(envOrDefault('POSTGRES_DB_PORT', '5432'), 10),
  database: envOrDefault('POSTGRES_DB_NAME', 'app'),
  user: envOrDefault('POSTGRES_DB_USER', 'app'),
  password: envOrDefault('POSTGRES_DB_PASSWORD', 'app'),
});

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
  const result = await pool.query<OrderRow>(
    'SELECT * FROM orders WHERE order_number = $1',
    [orderNumber]
  );
  return result.rows[0] || null;
}

export async function findAllOrders(orderNumberFilter?: string): Promise<OrderRow[]> {
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

export async function updateOrderStatus(orderNumber: string, status: string): Promise<void> {
  await pool.query(
    'UPDATE orders SET status = $1 WHERE order_number = $2',
    [status, orderNumber]
  );
}

export async function insertCoupon(coupon: {
  code: string;
  discountRate: number;
  validFrom?: Date | null;
  validTo?: Date | null;
  usageLimit?: number | null;
}): Promise<void> {
  await pool.query(
    `INSERT INTO coupons (code, discount_rate, valid_from, valid_to, usage_limit, used_count)
     VALUES ($1, $2, $3, $4, $5, 0)`,
    [coupon.code, coupon.discountRate, coupon.validFrom ?? null, coupon.validTo ?? null, coupon.usageLimit ?? null]
  );
}

export async function findCouponByCode(code: string): Promise<CouponRow | null> {
  const result = await pool.query<CouponRow>(
    'SELECT * FROM coupons WHERE code = $1',
    [code]
  );
  return result.rows[0] || null;
}

export async function incrementCouponUsage(code: string): Promise<void> {
  await pool.query(
    'UPDATE coupons SET used_count = used_count + 1 WHERE code = $1',
    [code]
  );
}

export async function findAllCoupons(): Promise<CouponRow[]> {
  const result = await pool.query<CouponRow>('SELECT * FROM coupons ORDER BY id');
  return result.rows;
}

// Releases the connection pool. Tests must call this before stopping a
// Testcontainers Postgres, otherwise the container shutdown surfaces as an
// unhandled "terminating connection due to administrator command" pool error.
export async function closePool(): Promise<void> {
  await pool.end();
}
