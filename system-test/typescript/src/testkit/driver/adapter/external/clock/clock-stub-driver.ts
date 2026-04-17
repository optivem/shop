import type { Result } from '../../../../common/result.js';
import type { ClockErrorResponse } from '../../../port/external/clock/dtos/ClockErrorResponse.js';
import type { GetTimeResponse } from '../../../port/external/clock/dtos/GetTimeResponse.js';
import type { ReturnsTimeRequest } from '../../../port/external/clock/dtos/ReturnsTimeRequest.js';
import type { ClockDriver } from '../../../port/external/clock/clock-driver.js';
import { ClockStubClient } from './client/ClockStubClient.js';

export class ClockStubDriver implements ClockDriver {
  private readonly client: ClockStubClient;

  constructor(baseUrl: string) {
    this.client = new ClockStubClient(baseUrl);
  }

  async goToClock(): Promise<Result<void, ClockErrorResponse>> {
    return this.client.checkHealth();
  }

  async getTime(): Promise<Result<GetTimeResponse, ClockErrorResponse>> {
    return this.client.getTime();
  }

  async returnsTime(request: ReturnsTimeRequest): Promise<Result<void, ClockErrorResponse>> {
    return this.client.configureGetTime(request);
  }

  async close(): Promise<void> {
    await this.client.close();
  }
}
