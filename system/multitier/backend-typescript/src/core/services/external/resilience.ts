import type { ConfigService } from '@nestjs/config';
import {
  ExponentialBackoff,
  SamplingBreaker,
  TaskCancelledError,
  TimeoutStrategy,
  circuitBreaker,
  handleWhen,
  retry,
  timeout,
  wrap,
} from 'cockatiel';
import { HttpStatusError } from './fetch-json';

const DEFAULT_TIMEOUT_MS = '2000';
const DEFAULT_BREAKER_HALF_OPEN_AFTER_MS = '10000';

// Retry: 3 attempts in total, exponential backoff from 200ms with decorrelated jitter.
const RETRIES = 2;
const BACKOFF_INITIAL_DELAY_MS = 200;
const BACKOFF_MAX_DELAY_MS = 2000;

// Breaker: opens when more than half of at least 5 calls in the last 10s failed.
const BREAKER_FAILURE_THRESHOLD = 0.5;
const BREAKER_SAMPLING_DURATION_MS = 10_000;
const BREAKER_MINIMUM_CALLS = 5;

export interface ResilienceOptions {
  timeoutMs: number;
  halfOpenAfterMs: number;
}

/** An outbound call; it must pass the signal to fetch so a timed-out attempt really aborts. */
export type ExternalCall<T> = (signal: AbortSignal) => Promise<T>;

export type IdempotentCallPolicy = <T>(call: ExternalCall<T>) => Promise<T>;

export function resilienceOptionsFrom(
  configService: ConfigService,
): ResilienceOptions {
  return {
    timeoutMs: positiveInteger(
      'EXTERNAL_TIMEOUT_MS',
      configService.get<string>('EXTERNAL_TIMEOUT_MS', DEFAULT_TIMEOUT_MS),
    ),
    halfOpenAfterMs: positiveInteger(
      'EXTERNAL_BREAKER_HALF_OPEN_AFTER_MS',
      configService.get<string>(
        'EXTERNAL_BREAKER_HALF_OPEN_AFTER_MS',
        DEFAULT_BREAKER_HALF_OPEN_AFTER_MS,
      ),
    ),
  };
}

/**
 * Timeout per attempt, retry and a circuit breaker for one external system. Only for idempotent
 * calls (GETs), since a failed attempt is retried. Timeouts, network errors and 5xx count as
 * failures; 4xx and malformed responses are answers, so they are neither retried nor counted.
 */
export function idempotentCallPolicy(
  options: ResilienceOptions,
): IdempotentCallPolicy {
  // An open breaker (BrokenCircuitError) is not transient, so it fails fast instead of retrying.
  const retryPolicy = retry(handleWhen(isTransient), {
    maxAttempts: RETRIES,
    backoff: new ExponentialBackoff({
      initialDelay: BACKOFF_INITIAL_DELAY_MS,
      maxDelay: BACKOFF_MAX_DELAY_MS,
    }),
  });
  const breakerPolicy = circuitBreaker(handleWhen(isTransient), {
    halfOpenAfter: options.halfOpenAfterMs,
    breaker: new SamplingBreaker({
      threshold: BREAKER_FAILURE_THRESHOLD,
      duration: BREAKER_SAMPLING_DURATION_MS,
      minimumRps: BREAKER_MINIMUM_CALLS / (BREAKER_SAMPLING_DURATION_MS / 1000),
    }),
  });
  const timeoutPolicy = timeout(options.timeoutMs, TimeoutStrategy.Aggressive);
  const policy = wrap(retryPolicy, breakerPolicy, timeoutPolicy);
  return <T>(call: ExternalCall<T>) =>
    policy.execute(({ signal }) => call(signal));
}

function isTransient(error: unknown): boolean {
  if (error instanceof TaskCancelledError) {
    return true;
  }
  if (error instanceof HttpStatusError) {
    return error.status >= 500;
  }
  // fetch reports an unreachable host or a dropped connection as a TypeError, an abort by name.
  return (
    error instanceof TypeError ||
    (error instanceof Error &&
      (error.name === 'AbortError' || error.name === 'TimeoutError'))
  );
}

function positiveInteger(key: string, value: string): number {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${key} must be a positive integer, got: ${value}`);
  }
  return parsed;
}
