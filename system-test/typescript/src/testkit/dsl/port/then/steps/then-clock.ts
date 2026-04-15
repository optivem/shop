import type { ThenStep } from './then-step.js';

export interface ThenClock extends ThenStep<ThenClock> {
  hasTime(): ThenClock;
  hasTime(time: string): ThenClock;
}
