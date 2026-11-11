import { NextResponse } from 'next/server';
import { logger } from './logger';

const BASE_URL = 'https://api.my-company.example/errors';

// RFC 7807 problem details go out as application/problem+json, matching the Java and .NET
// backends. NextResponse.json only defaults Content-Type to application/json when none is set.
const PROBLEM_JSON_CONTENT_TYPE = 'application/problem+json';

function problemResponse(body: Record<string, unknown>, init: { status: number; headers?: Record<string, string> }) {
  return NextResponse.json(body, {
    ...init,
    headers: { ...init.headers, 'Content-Type': PROBLEM_JSON_CONTENT_TYPE },
  });
}

export interface FieldError {
  field: string;
  message: string;
  code?: string;
  rejectedValue?: unknown;
}

export function badRequestResponse(detail: string) {
  return problemResponse(
    {
      type: `${BASE_URL}/bad-request`,
      title: 'Bad Request',
      status: 400,
      detail,
      timestamp: new Date().toISOString(),
    },
    { status: 400 }
  );
}

export function validationErrorResponse(errors: FieldError[]) {
  return problemResponse(
    {
      type: `${BASE_URL}/validation-error`,
      title: 'Validation Error',
      status: 422,
      detail: 'The request contains one or more validation errors',
      timestamp: new Date().toISOString(),
      errors,
    },
    { status: 422 }
  );
}

export function generalValidationErrorResponse(detail: string) {
  return problemResponse(
    {
      type: `${BASE_URL}/validation-error`,
      title: 'Validation Error',
      status: 422,
      detail,
      timestamp: new Date().toISOString(),
    },
    { status: 422 }
  );
}

export function notFoundResponse(detail: string) {
  return problemResponse(
    {
      type: `${BASE_URL}/resource-not-found`,
      title: 'Resource Not Found',
      status: 404,
      detail,
      timestamp: new Date().toISOString(),
    },
    { status: 404 }
  );
}

export function conflictResponse() {
  return problemResponse(
    {
      type: `${BASE_URL}/conflict`,
      title: 'Conflict',
      status: 409,
      detail: 'The resource was modified concurrently, please retry',
      timestamp: new Date().toISOString(),
    },
    { status: 409 }
  );
}

export function unauthorizedResponse(detail: string) {
  return problemResponse(
    {
      type: `${BASE_URL}/unauthorized`,
      title: 'Unauthorized',
      status: 401,
      detail,
      timestamp: new Date().toISOString(),
    },
    { status: 401, headers: { 'WWW-Authenticate': 'Bearer' } }
  );
}

export function forbiddenResponse() {
  return problemResponse(
    {
      type: `${BASE_URL}/forbidden`,
      title: 'Forbidden',
      status: 403,
      detail: 'You do not have permission to perform this action',
      timestamp: new Date().toISOString(),
    },
    { status: 403 }
  );
}

// The ERP failure details (URL, status, body) are logged server-side only; the client is told
// product information is temporarily unavailable.
const ERP_UNAVAILABLE_DETAIL = "Product information can't be retrieved right now";
// Matches the ERP circuit breaker's default half-open delay (EXTERNAL_BREAKER_HALF_OPEN_AFTER_MS).
const ERP_RETRY_AFTER_SECONDS = '10';

export function erpUnavailableResponse(error: unknown) {
  logger.error('ERP unavailable', error);
  return problemResponse(
    {
      type: `${BASE_URL}/service-unavailable`,
      title: 'Service Unavailable',
      status: 503,
      detail: ERP_UNAVAILABLE_DETAIL,
      timestamp: new Date().toISOString(),
    },
    { status: 503, headers: { 'Retry-After': ERP_RETRY_AFTER_SECONDS } }
  );
}

// The exception is logged server-side only. Its message can carry internals (SQL, connection
// strings, stack fragments), so the client gets a generic detail instead.
const GENERAL_ERROR_DETAIL = 'An unexpected error occurred. Please try again later.';

export function internalErrorResponse(error: unknown) {
  logger.error('Unexpected error occurred', error);
  return problemResponse(
    {
      type: `${BASE_URL}/internal-server-error`,
      title: 'Internal Server Error',
      status: 500,
      detail: GENERAL_ERROR_DETAIL,
      timestamp: new Date().toISOString(),
    },
    { status: 500 }
  );
}
