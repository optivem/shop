import type { ClockDriver } from '../../../../../driver/port/external/clock/clock-driver.js';
import type { UseCaseContext } from '../../../use-case-context.js';
import { GoToClock } from './usecases/GoToClock.js';
import { GetTime } from './usecases/GetTime.js';
import { ReturnsTime } from './usecases/ReturnsTime.js';

export class ClockDsl {
  constructor(
    private readonly driver: ClockDriver,
    private readonly context: UseCaseContext,
  ) {}

  goToClock(): GoToClock {
    return new GoToClock(this.driver, this.context);
  }

  getTime(): GetTime {
    return new GetTime(this.driver, this.context);
  }

  returnsTime(): ReturnsTime {
    return new ReturnsTime(this.driver, this.context);
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
