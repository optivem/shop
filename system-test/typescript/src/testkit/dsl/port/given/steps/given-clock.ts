import type { GivenStep } from './given-step.js';

export interface GivenClock extends GivenStep {
  withTime(): GivenClock;
  withTime(time: string): GivenClock;
  withWeekday(): GivenClock;
  withWeekend(): GivenClock;
}
