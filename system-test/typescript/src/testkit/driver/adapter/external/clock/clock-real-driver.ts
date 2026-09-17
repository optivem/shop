import type { Result } from '../../../../common/result.js';
import { success } from '../../../../common/result.js';
import type { ClockErrorResponse } from '../../../port/external/clock/dtos/errors/ClockErrorResponse.js';
import type { GetTimeResponse } from '../../../port/external/clock/dtos/GetTimeResponse.js';
import type { ReturnsTimeRequest } from '../../../port/external/clock/dtos/ReturnsTimeRequest.js';
import type { ClockDriver } from '../../../port/external/clock/clock-driver.js';
import { ClockRealClient } from './client/ClockRealClient.js';

export class ClockRealDriver implements ClockDriver {
  private readonly client = new ClockRealClient();

  goToClock(): Promise<Result<void, ClockErrorResponse>> {
    return Promise.resolve(success(undefined));
  }

  async getTime(): Promise<Result<GetTimeResponse, ClockErrorResponse>> {
    return this.client.getTime();
  }

  returnsTime(_request: ReturnsTimeRequest): Promise<Result<void, ClockErrorResponse>> {
    return Promise.resolve(success(undefined));
  }

  close(): Promise<void> {
    return Promise.resolve();
  }
}
