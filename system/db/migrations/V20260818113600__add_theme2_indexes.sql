-- Theme 2: the indexes the pushed-down work needs.
--
-- Additive only. No table, column, or type changes -- every statement below is a new index on
-- columns that already exist, so the previous app version keeps working unchanged and this
-- migration is rollback-safe by construction.
--
-- Each index exists because one query asks for it. An index nobody's plan uses is a write-time tax
-- with no read-time return, so the list stops here.

-- BrowseOrderHistory's keyset page: ORDER BY order_timestamp DESC, order_number DESC with a
-- row-value predicate on the same tuple. The column order and the DESC/DESC direction have to match
-- the query's ORDER BY, or Postgres sorts instead of walking.
CREATE INDEX idx_orders_recent ON orders (order_timestamp DESC, order_number DESC);

-- RecallSku's set-based UPDATE: WHERE sku = ? AND status IN (...). sku first because it is the
-- selective half -- a recall touches one product and every status it is allowed to cancel.
CREATE INDEX idx_orders_sku_status ON orders (sku, status);

-- The sales report's revenue-by-country-and-month GROUP BY, which filters on status and groups by
-- country and the truncated timestamp. status leads because the report excludes exactly one status
-- and keeps the rest.
CREATE INDEX idx_orders_status_country_ts ON orders (status, country, order_timestamp);

-- The report's coupon-effectiveness LEFT JOIN, on orders.applied_coupon_code.
--
-- Partial, and that is the point worth pausing on: most orders carry no coupon, so a full index
-- would store a row per NULL and answer nothing with them. Indexing only the rows that do have a
-- coupon makes the index a fraction of the size and keeps it in cache -- and Postgres will use it
-- for any query whose predicate implies the WHERE clause, which the join's own
-- `o.applied_coupon_code = c.code` does.
CREATE INDEX idx_orders_applied_coupon ON orders (applied_coupon_code)
    WHERE applied_coupon_code IS NOT NULL;
