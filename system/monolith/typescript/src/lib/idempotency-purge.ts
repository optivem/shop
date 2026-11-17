import { purgeExpiredIdempotencyKeys } from './db';
import { expiryCutoff } from './idempotency';
import { logger } from './logger';

const DEFAULT_PURGE_INTERVAL_MS = 60 * 60 * 1000;
const STARTED = Symbol.for('myshop.idempotencyKeyPurge');

/**
 * Purges expired idempotency_keys rows every IDEMPOTENCY_PURGE_INTERVAL_MS (default one hour; the first run waits
 * one interval). Expired rows are already ignored and replaced on their next use; this only keeps the table from
 * growing without bound. Every instance runs it; the DELETE is idempotent, so overlapping runs are harmless.
 */
export function startIdempotencyKeyPurge(): void {
  const registry = globalThis as Record<symbol, unknown>;
  if (registry[STARTED]) {
    return;
  }
  registry[STARTED] = true;
  const configured = Number(process.env.IDEMPOTENCY_PURGE_INTERVAL_MS);
  const intervalMs = Number.isFinite(configured) && configured > 0 ? configured : DEFAULT_PURGE_INTERVAL_MS;
  setInterval(() => void purgeOnce(), intervalMs).unref();
}

export async function purgeOnce(now: Date = new Date()): Promise<number> {
  try {
    const deleted = await purgeExpiredIdempotencyKeys(expiryCutoff(now));
    if (deleted > 0) {
      logger.info(`Purged ${deleted} expired idempotency keys`);
    }
    return deleted;
  } catch (error) {
    logger.warn(`Purging expired idempotency keys failed; retrying next interval: ${String(error)}`);
    return 0;
  }
}
