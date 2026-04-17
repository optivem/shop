import { Result, success } from '../../../../common/result.js';
import { ClockErrorResponse } from '../../../port/external/clock/dtos/ClockErrorResponse.js';
import { GetTimeResponse } from '../../../port/external/clock/dtos/GetTimeResponse.js';
import { ReturnsTimeRequest } from '../../../port/external/clock/dtos/ReturnsTimeRequest.js';
import { ClockDriver } from '../../../port/external/clock/clock-driver.js';

export class ClockRealDriver implements ClockDriver {
  async goToClock(): Promise<Result<void, ClockErrorResponse>> {
    return success(undefined);
  }

  async getTime(): Promise<Result<GetTimeResponse, ClockErrorResponse>> {
    return success({ time: new Date().toISOString() });
  }

  async returnsTime(_request: ReturnsTimeRequest): Promise<Result<void, ClockErrorResponse>> {
    return success(undefined);
  }

  async close(): Promise<void> {}
}
