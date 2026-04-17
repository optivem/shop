import type { Result } from '../../../../../common/result.js';
import type { ClockDriver } from '../../../../../driver/port/external/clock/clock-driver.js';
import type { ClockErrorResponse } from '../../../../../driver/port/external/clock/dtos/ClockErrorResponse.js';
import type { GetTimeResponse } from '../../../../../driver/port/external/clock/dtos/GetTimeResponse.js';
import type { ReturnsTimeRequest } from '../../../../../driver/port/external/clock/dtos/ReturnsTimeRequest.js';

export class ClockDsl {
  constructor(private readonly driver: ClockDriver) {}

  async goToClock(): Promise<Result<void, ClockErrorResponse>> {
    return this.driver.goToClock();
  }

  async getTime(): Promise<Result<GetTimeResponse, ClockErrorResponse>> {
    return this.driver.getTime();
  }

  async returnsTime(request: ReturnsTimeRequest): Promise<Result<void, ClockErrorResponse>> {
    return this.driver.returnsTime(request);
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
