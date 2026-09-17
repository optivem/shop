import {
  ArgumentsHost,
  BadRequestException,
  Catch,
  ExceptionFilter,
  HttpException,
  Logger,
} from '@nestjs/common';
import type { Response } from 'express';
import { ValidationException } from '../../core/exceptions/validation.exception';
import { NotExistValidationException } from '../../core/exceptions/not-exist-validation.exception';
import { RequestValidationException } from './request-validation.exception';

const VALIDATION_ERROR_TYPE_URI =
  'https://api.my-company.example/errors/validation-error';
const RESOURCE_NOT_FOUND_TYPE_URI =
  'https://api.my-company.example/errors/resource-not-found';
const BAD_REQUEST_TYPE_URI =
  'https://api.my-company.example/errors/bad-request';
const INTERNAL_SERVER_ERROR_TYPE_URI =
  'https://api.my-company.example/errors/internal-server-error';

@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(GlobalExceptionFilter.name);

  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();

    if (exception instanceof RequestValidationException) {
      this.handleRequestValidationException(exception, response);
    } else if (exception instanceof NotExistValidationException) {
      this.handleNotExistValidationException(exception, response);
    } else if (exception instanceof ValidationException) {
      this.handleValidationException(exception, response);
    } else if (exception instanceof BadRequestException) {
      this.handleBadRequestException(response);
    } else if (exception instanceof HttpException) {
      this.handleHttpException(exception, response);
    } else {
      this.handleGeneralException(exception, response);
    }
  }

  private handleValidationException(
    ex: ValidationException,
    response: Response,
  ) {
    if (ex.fieldName === null) {
      const body: Record<string, unknown> = {
        type: VALIDATION_ERROR_TYPE_URI,
        title: 'Validation Error',
        status: 422,
        detail: ex.message,
        timestamp: new Date().toISOString(),
      };
      response.status(422).type('application/problem+json').json(body);
    } else {
      const body: Record<string, unknown> = {
        type: VALIDATION_ERROR_TYPE_URI,
        title: 'Validation Error',
        status: 422,
        detail: 'The request contains one or more validation errors',
        timestamp: new Date().toISOString(),
        errors: [{ field: ex.fieldName, message: ex.message }],
      };
      response.status(422).type('application/problem+json').json(body);
    }
  }

  private handleRequestValidationException(
    ex: RequestValidationException,
    response: Response,
  ) {
    const body = {
      type: VALIDATION_ERROR_TYPE_URI,
      title: 'Validation Error',
      status: 422,
      detail: ex.message,
      timestamp: new Date().toISOString(),
      errors: ex.fieldErrors,
    };
    response.status(422).type('application/problem+json').json(body);
  }

  private handleNotExistValidationException(
    ex: NotExistValidationException,
    response: Response,
  ) {
    const body = {
      type: RESOURCE_NOT_FOUND_TYPE_URI,
      title: 'Resource Not Found',
      status: 404,
      detail: ex.message,
      timestamp: new Date().toISOString(),
    };
    response.status(404).type('application/problem+json').json(body);
  }

  // Malformed JSON, or a body that is not an object.
  private handleBadRequestException(response: Response) {
    const body = {
      type: BAD_REQUEST_TYPE_URI,
      title: 'Bad Request',
      status: 400,
      detail: 'Invalid request format',
      timestamp: new Date().toISOString(),
    };
    response.status(400).type('application/problem+json').json(body);
  }

  private handleHttpException(ex: HttpException, response: Response) {
    const status = ex.getStatus();
    const body = {
      type: INTERNAL_SERVER_ERROR_TYPE_URI,
      title: 'Internal Server Error',
      status,
      detail: ex.message,
      timestamp: new Date().toISOString(),
    };
    response.status(status).type('application/problem+json').json(body);
  }

  private handleGeneralException(ex: unknown, response: Response) {
    const error = ex instanceof Error ? ex : new Error(String(ex));
    this.logger.error('Unexpected error occurred', error.stack);

    const body = {
      type: INTERNAL_SERVER_ERROR_TYPE_URI,
      title: 'Internal Server Error',
      status: 500,
      detail: `Internal server error: ${error.message}`,
      timestamp: new Date().toISOString(),
    };
    response.status(500).type('application/problem+json').json(body);
  }
}
