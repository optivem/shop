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
    CREATE TABLE IF NOT EXISTS orders (
      id BIGSERIAL PRIMARY KEY,
      order_number VARCHAR(255) NOT NULL UNIQUE,
      order_timestamp TIMESTAMPTZ NOT NULL,
      sku VARCHAR(255) NOT NULL,
      quantity INTEGER NOT NULL,
      unit_price NUMERIC(10,2) NOT NULL,
      total_price NUMERIC(10,2) NOT NULL,
      status VARCHAR(50) NOT NULL
    )
  `).then(() => {});
  return schemaPromise;
}

export interface OrderRow {
  id: number;
  order_number: string;
  order_timestamp: Date;
  sku: string;
  quantity: number;
  unit_price: string;
  total_price: string;
  status: string;
}

export async function insertOrder(order: {
  orderNumber: string;
  orderTimestamp: Date;
  sku: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  status: string;
}): Promise<void> {
  await ensureSchema();
  await pool.query(
    `INSERT INTO orders (order_number, order_timestamp, sku, quantity, unit_price, total_price, status)
     VALUES ($1, $2, $3, $4, $5, $6, $7)`,
    [order.orderNumber, order.orderTimestamp, order.sku, order.quantity, order.unitPrice, order.totalPrice, order.status]
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
