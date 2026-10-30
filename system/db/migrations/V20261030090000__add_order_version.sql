-- Optimistic locking: version counter on orders, incremented on every update.
--
-- Additive only: one column with a default, so previous app versions (which never read or write it)
-- keep working -- rollback-safe by construction.
ALTER TABLE orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
