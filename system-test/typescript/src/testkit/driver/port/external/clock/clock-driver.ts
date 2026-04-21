import { Result } from '../../../../common/result.js';
import { ClockErrorResponse } from './dtos/errors/ClockErrorResponse.js';
import { GetTimeResponse } from './dtos/GetTimeResponse.js';
import { ReturnsTimeRequest } from './dtos/ReturnsTimeRequest.js';

export interface ClockDriver {
  goToClock(): Promise<Result<void, ClockErrorResponse>>;
  getTime(): Promise<Result<GetTimeResponse, ClockErrorResponse>>;
  returnsTime(request: ReturnsTimeRequest): Promise<Result<void, ClockErrorResponse>>;
  close(): Promise<void>;
}
