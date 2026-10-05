-- Order ownership: the authenticated user (token sub) who placed the order, plus their username for display.
--
-- Additive only: two nullable columns and an index. Orders that exist before this migration keep a NULL
-- owner (visible to admins only), and every previous app version keeps working because it never reads
-- or writes the new columns -- rollback-safe by construction.
ALTER TABLE orders ADD COLUMN owner VARCHAR(255) NULL;
ALTER TABLE orders ADD COLUMN owner_name VARCHAR(255) NULL;

-- A customer's order history and lookup filter on owner and sort by newest first.
CREATE INDEX idx_orders_owner ON orders (owner, order_timestamp DESC);
