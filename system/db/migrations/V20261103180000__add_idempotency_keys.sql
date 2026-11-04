-- Idempotent POST /api/orders: one row per (owner, Idempotency-Key), written in the same transaction as
-- the order it answers for, so a replay returns the stored response instead of placing a second order.
--
-- owner is the token subject of the caller (empty string for an anonymous caller), so two users can use
-- the same key without colliding. request_hash is the SHA-256 fingerprint of the request; a replay with a
-- different fingerprint is rejected. Rows live 24 hours from created_at; an expired row is replaced on
-- its next use.
--
-- Additive only: a new table that previous app versions never read or write -- rollback-safe by
-- construction.
CREATE TABLE idempotency_keys (
  id BIGSERIAL PRIMARY KEY,
  owner VARCHAR(255) NOT NULL,
  idempotency_key VARCHAR(255) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  response_status INTEGER NOT NULL,
  response_body TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  CONSTRAINT uq_idempotency_keys_owner_key UNIQUE (owner, idempotency_key)
);

-- Finding expired rows for a later purge.
CREATE INDEX idx_idempotency_keys_created_at ON idempotency_keys (created_at);
