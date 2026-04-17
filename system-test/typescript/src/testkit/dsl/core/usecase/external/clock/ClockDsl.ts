import type { Result } from '../../../../../common/result.js';
import type { ClockDriver } from '../../../../../driver/port/external/clock/clock-driver.js';
import type { ClockErrorResponse } from '../../../../../driver/port/external/clock/dtos/ClockErrorResponse.js';
import type { UseCaseContext } from '../../../use-case-context.js';

export class ClockDsl {
  constructor(
    private readonly driver: ClockDriver,
    private readonly context: UseCaseContext,
  ) {}

  async goToClock(): Promise<Result<void, ClockErrorResponse>> {
    return this.driver.goToClock();
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
