import type { ClockDriver } from '../../../../../../driver/port/external/clock/clock-driver.js';
import type { GetTimeResponse } from '../../../../../../driver/port/external/clock/dtos/GetTimeResponse.js';
import { success, failure, type Result } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../../driver/port/shop/dtos/errors/SystemError.js';
import { UseCaseResult } from '../../../../shared/use-case-result.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';
import { BaseClockUseCase } from './base/BaseClockUseCase.js';
import { GetTimeVerification } from './GetTimeVerification.js';

export class GetTime extends BaseClockUseCase<GetTimeResponse, GetTimeVerification> {
  constructor(driver: ClockDriver, context: UseCaseContext) {
    super(driver, context);
  }

  async execute(): Promise<UseCaseResult<GetTimeResponse, GetTimeVerification>> {
    const driverResult = await this.driver.getTime();
    const result: Result<GetTimeResponse, SystemError> = driverResult.success
      ? success(driverResult.value)
      : failure({ message: driverResult.error.message ?? 'Clock error', fieldErrors: [] });

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new GetTimeVerification(response, ctx),
    );
  }
}
