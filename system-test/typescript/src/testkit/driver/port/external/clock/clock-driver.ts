import { Result } from '../../../../common/result.js';
import {
  ErrorResponse,
  GetTimeResponse,
  ReturnsTimeRequest,
} from '../../../../common/dtos.js';

export interface ClockDriver {
  goToClock(): Promise<Result<void, ErrorResponse>>;
  getTime(): Promise<Result<GetTimeResponse, ErrorResponse>>;
  returnsTime(request: ReturnsTimeRequest): Promise<Result<void, ErrorResponse>>;
  close(): Promise<void>;
}
