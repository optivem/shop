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

const VALIDATION_ERROR_TYPE_URI =
  'https://api.optivem.com/errors/validation-error';
const RESOURCE_NOT_FOUND_TYPE_URI =
  'https://api.optivem.com/errors/resource-not-found';
const BAD_REQUEST_TYPE_URI = 'https://api.optivem.com/errors/bad-request';
const INTERNAL_SERVER_ERROR_TYPE_URI =
  'https://api.optivem.com/errors/internal-server-error';

interface ValidationError {
  field: string;
  message: string;
  code?: string | null;
  rejectedValue?: unknown;
}

@Catch()
export class GlobalExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(GlobalExceptionFilter.name);

  catch(exception: unknown, host: ArgumentsHost) {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();

    if (exception instanceof NotExistValidationException) {
      this.handleNotExistValidationException(exception, response);
    } else if (exception instanceof ValidationException) {
      this.handleValidationException(exception, response);
    } else if (exception instanceof BadRequestException) {
      this.handleBadRequestException(exception, response);
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
    if (ex.fieldName !== null) {
      const body: Record<string, unknown> = {
        type: VALIDATION_ERROR_TYPE_URI,
        title: 'Validation Error',
        status: 422,
        detail: 'The request contains one or more validation errors',
        timestamp: new Date().toISOString(),
        errors: [{ field: ex.fieldName, message: ex.message }],
      };
      response.status(422).json(body);
    } else {
      const body: Record<string, unknown> = {
        type: VALIDATION_ERROR_TYPE_URI,
        title: 'Validation Error',
        status: 422,
        detail: ex.message,
        timestamp: new Date().toISOString(),
      };
      response.status(422).json(body);
    }
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
    response.status(404).json(body);
  }

  private handleBadRequestException(
    ex: BadRequestException,
    response: Response,
  ) {
    const exResponse = ex.getResponse() as Record<string, unknown>;

    // Check if this is a validation pipe error (class-validator)
    if (Array.isArray(exResponse?.validationErrors)) {
      const rawBody = exResponse.rawBody as Record<string, unknown> | undefined;
      const errors = this.buildValidationErrors(
        exResponse.validationErrors as ValidationErrorDetail[],
        rawBody,
      );

      const body = {
        type: VALIDATION_ERROR_TYPE_URI,
        title: 'Validation Error',
        status: 422,
        detail: 'The request contains one or more validation errors',
        timestamp: new Date().toISOString(),
        errors,
      };
      response.status(422).json(body);
      return;
    }

    // Generic bad request (e.g., malformed JSON)
    const body = {
      type: BAD_REQUEST_TYPE_URI,
      title: 'Bad Request',
      status: 400,
      detail: 'Invalid request format',
      timestamp: new Date().toISOString(),
    };
    response.status(400).json(body);
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
    response.status(status).json(body);
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
    response.status(500).json(body);
  }

  private buildValidationErrors(
    validationErrors: ValidationErrorDetail[],
    rawBody: Record<string, unknown> | undefined,
  ): ValidationError[] {
    const errors: ValidationError[] = [];

    for (const err of validationErrors) {
      const field = err.field;
      const constraints = err.constraints || {};
      const constraintKeys = Object.keys(constraints);

      if (constraintKeys.length === 0) {
        continue;
      }

      // Check if this is a type mismatch scenario
      const isTypeMismatch = this.isTypeMismatch(
        field,
        err.expectedType,
        rawBody,
      );

      if (isTypeMismatch === 'empty') {
        const emptyMessage =
          constraints['isNotEmpty'] ||
          constraints[constraintKeys[constraintKeys.length - 1]];
        errors.push({
          field,
          message: emptyMessage,
          code: null,
          rejectedValue: null,
        });
      } else if (isTypeMismatch === 'type_mismatch') {
        const typeMismatchMessage =
          err.typeMismatchMessage || constraints[constraintKeys[0]];
        errors.push({
          field,
          message: typeMismatchMessage,
          code: 'TYPE_MISMATCH',
          rejectedValue: null,
        });
      } else {
        // Normal validation error - use the first constraint message
        for (const key of constraintKeys) {
          errors.push({
            field,
            message: constraints[key],
            code: key === 'isNotEmpty' ? 'NotBlank' : null,
            rejectedValue: null,
          });
          break; // Only report the first error per field
        }
      }
    }

    return errors;
  }

  private isTypeMismatch(
    field: string,
    expectedType: string | undefined,
    rawBody: Record<string, unknown> | undefined,
  ): 'empty' | 'type_mismatch' | 'normal' {
    if (!rawBody || expectedType !== 'integer') {
      return 'normal';
    }

    const rawValue = rawBody[field];

    // null or undefined → empty
    if (rawValue === null || rawValue === undefined) {
      return 'empty';
    }

    // string → check if empty/whitespace, parseable integer, or type mismatch
    if (typeof rawValue === 'string') {
      if (rawValue.trim() === '') {
        return 'empty';
      }
      const num = Number(rawValue);
      if (Number.isInteger(num)) {
        return 'normal';
      }
      return 'type_mismatch';
    }

    // number that's not an integer → type mismatch (e.g., 1.5)
    if (typeof rawValue === 'number' && !Number.isInteger(rawValue)) {
      return 'type_mismatch';
    }

    // boolean → type mismatch
    if (typeof rawValue === 'boolean') {
      return 'type_mismatch';
    }

    return 'normal';
  }
}

interface ValidationErrorDetail {
  field: string;
  constraints: Record<string, string>;
  expectedType?: string;
  typeMismatchMessage?: string;
}
