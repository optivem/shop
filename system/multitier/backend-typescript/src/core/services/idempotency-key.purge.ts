import {
  Injectable,
  Logger,
  type OnApplicationBootstrap,
  type OnApplicationShutdown,
} from '@nestjs/common';
import { IdempotencyKeyStore } from './idempotency-key.store';

const DEFAULT_PURGE_INTERVAL_MS = 60 * 60 * 1000;

/**
 * Purges expired idempotency_keys rows every IDEMPOTENCY_PURGE_INTERVAL_MS (default one hour; the
 * first run waits one interval). Every instance runs it; the DELETE is idempotent, so overlapping
 * runs are harmless. The timer is unref'd so it never keeps the process alive.
 */
@Injectable()
export class IdempotencyKeyPurge
  implements OnApplicationBootstrap, OnApplicationShutdown
{
  private readonly logger = new Logger(IdempotencyKeyPurge.name);
  private timer?: NodeJS.Timeout;

  constructor(private readonly store: IdempotencyKeyStore) {}

  onApplicationBootstrap(): void {
    const configured = Number(process.env.IDEMPOTENCY_PURGE_INTERVAL_MS);
    const intervalMs =
      Number.isFinite(configured) && configured > 0
        ? configured
        : DEFAULT_PURGE_INTERVAL_MS;
    this.timer = setInterval(() => void this.purge(), intervalMs);
    this.timer.unref();
  }

  onApplicationShutdown(): void {
    clearInterval(this.timer);
  }

  async purge(): Promise<void> {
    try {
      const deleted = await this.store.purgeExpired(new Date());
      if (deleted > 0) {
        this.logger.log(`Purged ${deleted} expired idempotency keys`);
      }
    } catch (error) {
      this.logger.warn(
        `Purging expired idempotency keys failed; retrying next interval: ${String(error)}`,
      );
    }
  }
}
