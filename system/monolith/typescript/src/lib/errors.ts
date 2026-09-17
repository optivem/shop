import { NextResponse } from 'next/server';

const BASE_URL = 'https://api.my-company.example/errors';

export interface FieldError {
  field: string;
  message: string;
  code?: string;
  rejectedValue?: unknown;
}

export function badRequestResponse(detail: string) {
  return NextResponse.json(
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
  return NextResponse.json(
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
  return NextResponse.json(
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
  return NextResponse.json(
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

export function internalErrorResponse(message: string) {
  return NextResponse.json(
    {
      type: `${BASE_URL}/internal-server-error`,
      title: 'Internal Server Error',
      status: 500,
      detail: `Internal server error: ${message}`,
      timestamp: new Date().toISOString(),
    },
    { status: 500 }
  );
}
