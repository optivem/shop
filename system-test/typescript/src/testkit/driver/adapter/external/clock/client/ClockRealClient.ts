import { Result, success } from '../../../../../common/result.js';
import type { ClockErrorResponse } from '../../../../port/external/clock/dtos/errors/ClockErrorResponse.js';
import type { GetTimeResponse } from '../../../../port/external/clock/dtos/GetTimeResponse.js';

export class ClockRealClient {
  async getTime(): Promise<Result<GetTimeResponse, ClockErrorResponse>> {
    return success({ time: new Date().toISOString() });
  }
}
