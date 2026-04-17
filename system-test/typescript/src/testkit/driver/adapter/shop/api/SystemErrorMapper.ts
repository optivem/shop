import type { SystemError } from '../../../port/shop/dtos/SystemError.js';
import type { ProblemDetailResponse } from './client/dtos/errors/ProblemDetailResponse.js';

export class SystemErrorMapper {
  private constructor() {
    // Utility class
  }

  static from(problemDetail: ProblemDetailResponse): SystemError {
    const message = problemDetail.detail ?? 'Request failed';
    const errors = problemDetail.errors ?? [];
    return {
      message,
      fieldErrors: errors.map((e) => ({
        field: e.field,
        message: e.message,
      })),
    };
  }
}
