import { Result, success } from '../common/result';
import { ErrorResponse, GetTimeResponse, ReturnsTimeRequest } from '../common/dtos';
import { ClockDriver } from './types';

export class ClockRealDriver implements ClockDriver {
  async goToClock(): Promise<Result<void, ErrorResponse>> {
    return success(undefined);
  }

  async getTime(): Promise<Result<GetTimeResponse, ErrorResponse>> {
    return success({ time: new Date().toISOString() });
  }

  async returnsTime(_request: ReturnsTimeRequest): Promise<Result<void, ErrorResponse>> {
    return success(undefined);
  }

  async close(): Promise<void> {}
}
