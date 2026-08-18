-- Demo volume seed for theme 2 ("the database is barred from the work it does best").
--
-- NOT a Flyway migration, and deliberately not under ../migrations/: this must never run in CI or
-- in production. It exists so the theme-2 demonstrations in system/multitier/backend-clean-java can
-- be measured against a table that is big enough for the difference to be visible, rather than
-- asserted about.
--
-- Run it by hand:
--   psql "$DATABASE_URL" -v ON_ERROR_STOP=1 --single-transaction -f system/db/seed/demo-volume.sql
--
-- ...or let the benchmark harness load it (it does, from
-- system/multitier/backend-clean-java/src/benchmark, via `./gradlew benchmark`).
--
-- Three properties this script deliberately has:
--
--   1. **Deterministic.** Every value is derived from the generate_series counter, never from
--      random(). Before-numbers and after-numbers are therefore taken over byte-identical data,
--      which is the only way the comparison means anything.
--   2. **Idempotent.** Re-running it replaces its own rows and touches nothing else. Everything it
--      writes is prefixed DEMO-, so it cannot collide with rows a test or a demo created.
--   3. **Schema-compatible.** It writes only columns that already exist in
--      V20260514085249__init.sql. This plan changes no schema.
--
-- Values are chosen so that every row survives the domain model's own validation, because today's
-- read path builds domain objects out of these rows: coupon discount_rate is always > 0 (Coupon's
-- constructor rejects zero), valid_to is never before valid_from (ValidityPeriod's rule), and
-- used_count is never negative (UsageQuota's rule). That constraint is itself the point of Chunk R:
-- a read path that can fail on a write-side invariant is a bug, and this file has to tiptoe around
-- it.

DELETE FROM orders  WHERE order_number LIKE 'DEMO-ORD-%';
DELETE FROM coupons WHERE code         LIKE 'DEMO-CPN-%';

-- 300 coupons. used_count is filled in at the end from the orders that actually reference them, so
-- the coupon-effectiveness report (Chunk B4) has something honest to aggregate.
INSERT INTO coupons (code, discount_rate, valid_from, valid_to, usage_limit, used_count)
SELECT
    'DEMO-CPN-' || lpad(c::text, 4, '0'),
    -- 0.0500 .. 0.3000, never zero: Coupon rejects a discount rate of zero.
    round((5 + (c % 26))::numeric / 100, 4),
    TIMESTAMPTZ '2024-01-01 00:00:00+00' + ((c % 365) * INTERVAL '1 day'),
    -- Two thirds open-ended, one third bounded — and always after valid_from.
    CASE WHEN c % 3 = 0 THEN TIMESTAMPTZ '2027-12-31 23:59:59+00' ELSE NULL END,
    -- One third unlimited (NULL), the rest capped somewhere between 50 and 249.
    CASE WHEN c % 3 = 1 THEN NULL ELSE 50 + (c % 200) END,
    0
FROM generate_series(1, 300) AS c;

-- 100,000 orders spread over ~19 months, across 8 countries, 50 SKUs and all three statuses.
-- 30% carry a coupon. The pricing chain is arithmetically consistent with OrderPricing, so a row
-- read back through the domain model is a row the application could have written.
WITH raw AS (
    SELECT
        n,
        'DEMO-ORD-' || lpad(n::text, 6, '0')                              AS order_number,
        TIMESTAMPTZ '2024-01-01 00:00:00+00' + (n * INTERVAL '10 minutes') AS order_timestamp,
        (ARRAY['US','DE','FR','GB','JP','BR','CA','AU'])[(n % 8)::int + 1] AS country,
        'SKU-' || lpad(((n % 50) + 1)::text, 3, '0')                      AS sku,
        ((n % 5) + 1)::int                                                AS quantity,
        round((10 + (n % 490))::numeric / 10, 2)                          AS unit_price,
        round((5 + (n % 21))::numeric / 100, 4)                           AS tax_rate,
        CASE WHEN n % 10 = 0 THEN 'CANCELLED'
             WHEN n % 10 < 4 THEN 'DELIVERED'
             ELSE 'PLACED' END                                            AS status,
        CASE WHEN n % 10 < 3
             THEN 'DEMO-CPN-' || lpad(((n % 300) + 1)::text, 4, '0')
             END                                                          AS applied_coupon_code,
        -- Must equal the referenced coupon's own rate, or the row would describe a discount that
        -- coupon never granted.
        CASE WHEN n % 10 < 3
             THEN round((5 + (((n % 300) + 1) % 26))::numeric / 100, 4)
             ELSE 0 END                                                   AS discount_rate
    FROM generate_series(1, 100000) AS n
),
priced AS (
    SELECT
        raw.*,
        round(unit_price * quantity, 2) AS base_price
    FROM raw
),
discounted AS (
    SELECT
        priced.*,
        round(base_price * discount_rate, 2) AS discount_amount
    FROM priced
),
subtotalled AS (
    SELECT
        discounted.*,
        base_price - discount_amount AS subtotal_price
    FROM discounted
)
INSERT INTO orders (order_number, order_timestamp, country, sku, quantity, unit_price, base_price,
                    discount_rate, discount_amount, subtotal_price, tax_rate, tax_amount,
                    total_price, applied_coupon_code, status)
SELECT
    order_number,
    order_timestamp,
    country,
    sku,
    quantity,
    unit_price,
    base_price,
    discount_rate,
    discount_amount,
    subtotal_price,
    tax_rate,
    round(subtotal_price * tax_rate, 2),
    subtotal_price + round(subtotal_price * tax_rate, 2),
    applied_coupon_code,
    status
FROM subtotalled;

-- used_count comes from the orders, not from a guess. Cancelled orders still count as a use: the
-- current application never gives a redemption back, and the seed must not invent behaviour the
-- code does not have. Some coupons end up at or over their usage_limit as a result — that is
-- realistic, and it is what makes the exhausted-coupon path (Chunk A3) demoable.
UPDATE coupons c
SET used_count = used.uses
FROM (
    SELECT applied_coupon_code AS code, COUNT(*) AS uses
    FROM orders
    WHERE applied_coupon_code LIKE 'DEMO-CPN-%'
    GROUP BY applied_coupon_code
) AS used
WHERE c.code = used.code;

ANALYZE coupons;
ANALYZE orders;
