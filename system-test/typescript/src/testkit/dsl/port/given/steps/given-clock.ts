import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenClock {
  withTime(time?: string): GivenClock;
  withWeekday(): GivenClock;
  withWeekend(): GivenClock;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
