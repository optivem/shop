import type { ClockDriver } from '../../../../../../driver/port/external/clock/clock-driver.js';
import type { ReturnsTimeRequest } from '../../../../../../driver/port/external/clock/dtos/ReturnsTimeRequest.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/shop/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import { VoidVerification } from '../../../../shared/void-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseClockUseCase } from './base/BaseClockUseCase.js';

export class ReturnsTime extends BaseClockUseCase<void, VoidVerification> {
  private _time: string = '';

  constructor(driver: ClockDriver, context: UseCaseContext) {
    super(driver, context);
  }

  time(time: string): this {
    this._time = time;
    return this;
  }

  async execute(): Promise<UseCaseResult<void, VoidVerification>> {
    const request: ReturnsTimeRequest = {
      time: this._time,
    };
    const driverResult = await this.driver.returnsTime(request);
    const result: Result<void, SystemError> = driverResult.success
      ? success(undefined)
      : failure({ message: driverResult.error.message ?? 'Clock error', fieldErrors: [] });

    return new UseCaseResult(
      result,
      this.context,
      (_, ctx) => new VoidVerification(undefined, ctx),
    );
  }
}
