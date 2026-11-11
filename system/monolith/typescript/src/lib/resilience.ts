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
import { envOrDefault } from './env';

const EXTERNAL_TIMEOUT_MS = () => positiveInteger('EXTERNAL_TIMEOUT_MS', envOrDefault('EXTERNAL_TIMEOUT_MS', '2000'));
const EXTERNAL_BREAKER_HALF_OPEN_AFTER_MS = () =>
  positiveInteger('EXTERNAL_BREAKER_HALF_OPEN_AFTER_MS', envOrDefault('EXTERNAL_BREAKER_HALF_OPEN_AFTER_MS', '10000'));

// Retry: 3 attempts in total, exponential backoff from 200ms with decorrelated jitter.
const RETRIES = 2;
const BACKOFF_INITIAL_DELAY_MS = 200;
const BACKOFF_MAX_DELAY_MS = 2000;

// Breaker: opens when more than half of at least 5 calls in the last 10s failed.
const BREAKER_FAILURE_THRESHOLD = 0.5;
const BREAKER_SAMPLING_DURATION_MS = 10_000;
const BREAKER_MINIMUM_CALLS = 5;

/** A non-2xx answer from an external system; 5xx is retried and trips the breaker, 4xx is not. */
export class HttpStatusError extends Error {
  constructor(
    message: string,
    readonly status: number
  ) {
    super(message);
    this.name = 'HttpStatusError';
  }
}

export type ExternalSystem = 'erp' | 'clock' | 'tax';

/** An outbound call; it must pass the signal to fetch so a timed-out attempt really aborts. */
export type ExternalCall<T> = (signal: AbortSignal) => Promise<T>;

type IdempotentCallPolicy = <T>(call: ExternalCall<T>) => Promise<T>;

// One breaker per external system, shared by every call to it in this process.
const policies = new Map<ExternalSystem, IdempotentCallPolicy>();

/**
 * Runs an idempotent call (a GET) to an external system with a timeout per attempt, retry and that
 * system's circuit breaker. Timeouts, network errors and 5xx count as failures; 4xx and malformed
 * responses are answers, so they are neither retried nor counted. An open breaker fails fast with
 * BrokenCircuitError, a timed-out attempt with TaskCancelledError.
 */
export function callIdempotent<T>(system: ExternalSystem, call: ExternalCall<T>): Promise<T> {
  let policy = policies.get(system);
  if (!policy) {
    policy = idempotentCallPolicy();
    policies.set(system, policy);
  }
  return policy(call);
}

/** Forgets every breaker's state, so each test starts with closed breakers. */
export function resetExternalCallPolicies(): void {
  policies.clear();
}

function idempotentCallPolicy(): IdempotentCallPolicy {
  // An open breaker (BrokenCircuitError) is not transient, so it fails fast instead of retrying.
  const retryPolicy = retry(handleWhen(isTransient), {
    maxAttempts: RETRIES,
    backoff: new ExponentialBackoff({ initialDelay: BACKOFF_INITIAL_DELAY_MS, maxDelay: BACKOFF_MAX_DELAY_MS }),
  });
  const breakerPolicy = circuitBreaker(handleWhen(isTransient), {
    halfOpenAfter: EXTERNAL_BREAKER_HALF_OPEN_AFTER_MS(),
    breaker: new SamplingBreaker({
      threshold: BREAKER_FAILURE_THRESHOLD,
      duration: BREAKER_SAMPLING_DURATION_MS,
      minimumRps: BREAKER_MINIMUM_CALLS / (BREAKER_SAMPLING_DURATION_MS / 1000),
    }),
  });
  const timeoutPolicy = timeout(EXTERNAL_TIMEOUT_MS(), TimeoutStrategy.Aggressive);
  const policy = wrap(retryPolicy, breakerPolicy, timeoutPolicy);
  return <T>(call: ExternalCall<T>) => policy.execute(({ signal }) => call(signal));
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
    (error instanceof Error && (error.name === 'AbortError' || error.name === 'TimeoutError'))
  );
}

function positiveInteger(key: string, value: string): number {
  const parsed = Number(value);
  if (!Number.isInteger(parsed) || parsed <= 0) {
    throw new Error(`${key} must be a positive integer, got: ${value}`);
  }
  return parsed;
}
