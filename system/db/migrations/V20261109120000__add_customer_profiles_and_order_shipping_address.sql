-- Customer profile and shipping address on orders (UC-010 Manage Profile, amended UC-001 Place Order).
--
-- customer_profiles: one row per customer, keyed by the token subject (owner), holding the name and the
-- default shipping address. The email is not stored: it always comes from the sign-in account.
--
-- orders.shipping_address: the address an order was placed with, as a JSON object
-- {firstName, lastName, street, postalCode, city, country}. NULL for orders placed before this migration
-- and for Quick orders, which take only a country. The tax country stays in orders.country.
--
-- Additive only: a new table and a nullable column that previous app versions never read or write --
-- rollback-safe by construction.
CREATE TABLE customer_profiles (
  id BIGSERIAL PRIMARY KEY,
  owner VARCHAR(255) NOT NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  street VARCHAR(255) NOT NULL,
  postal_code VARCHAR(255) NOT NULL,
  city VARCHAR(255) NOT NULL,
  country VARCHAR(255) NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_customer_profiles_owner UNIQUE (owner)
);

ALTER TABLE orders ADD COLUMN shipping_address JSONB NULL;

-- The admin customer filter on GET /api/orders?customer= matches on owner_name, newest first.
CREATE INDEX idx_orders_owner_name ON orders (owner_name, order_timestamp DESC);
