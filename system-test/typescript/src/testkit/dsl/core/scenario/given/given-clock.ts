import { DEFAULTS } from '../defaults.js';
import type { ClockConfig } from '../scenario-context.js';
import type { ThenContractStage } from '../then/then-contract.js';
import type { WhenStage } from '../when/when-stage.js';
import type { GivenStage } from './given-stage.js';
import type { GivenClock as IGivenClock } from '../../../port/given/steps/given-clock.js';
import { nonEmptyOr } from '../../../../common/fallback.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class GivenClock implements IGivenClock {
  constructor(
    private readonly stage: GivenStage,
    private readonly config: ClockConfig,
  ) {}

  withTime(time?: string): this {
    this.config.time = nonEmptyOr(time, DEFAULTS.CLOCK_TIME);
    return this;
  }

  withWeekday(): this {
    this.config.time = DEFAULTS.WEEKDAY_TIME;
    return this;
  }

  withWeekend(): this {
    this.config.time = DEFAULTS.WEEKEND_TIME;
    return this;
  }

  and(): GivenStage {
    return this.stage;
  }

  when(): WhenStage {
    return this.stage.when();
  }

  then(): ThenContractStage;
  then(...args: unknown[]): ThenContractStage {
    assertNotAwaited(args);
    return this.stage.then();
  }
}
