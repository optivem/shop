import type { SystemError } from '../../port/dtos/errors/SystemError.js';
import type { ProblemDetailResponse } from './client/dtos/errors/ProblemDetailResponse.js';

type ProblemDetailFieldError = NonNullable<ProblemDetailResponse['errors']>[number];

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

  // Always a complete SystemError, even when the body is not a problem detail (e.g. an HTML 502 from a proxy),
  // so an error assertion fails on the value it checks rather than with a TypeError.
  static fromResponse(this: void, status: number, body: unknown): SystemError {
    const problemDetail = toProblemDetail(body);
    return SystemErrorMapper.from({ ...problemDetail, detail: problemDetail.detail ?? `Request failed (HTTP ${status})` });
  }
}

function toProblemDetail(body: unknown): ProblemDetailResponse {
  if (!isObject(body)) {
    return {};
  }
  return {
    detail: typeof body.detail === 'string' ? body.detail : undefined,
    errors: Array.isArray(body.errors) ? body.errors.filter(isFieldError) : [],
  };
}

function isFieldError(value: unknown): value is ProblemDetailFieldError {
  return isObject(value) && typeof value.field === 'string' && typeof value.message === 'string';
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
