import type { ThenFailure } from './then-failure.js';

/** Place-order failures can also be checked for graceful degradation when an external system misbehaves. */
export interface ThenPlaceOrderFailure extends ThenFailure {
  /** 503 Service Unavailable with a whole-seconds Retry-After and a problem+json body; API channel only. */
  serviceUnavailable(): this;
  /** The failure came back within `seconds`: the system gave up on its own instead of hanging. */
  failedWithinSeconds(seconds: number): this;
}
