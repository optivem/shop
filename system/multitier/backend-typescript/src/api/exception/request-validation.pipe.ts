import {
  ArgumentMetadata,
  BadRequestException,
  Injectable,
  ValidationError,
  ValidationPipe,
} from '@nestjs/common';
import {
  FieldError,
  RequestValidationException,
} from './request-validation.exception';
import { TYPE_MISMATCH } from '../../core/dtos/request-parsing';

@Injectable()
export class RequestValidationPipe extends ValidationPipe {
  constructor() {
    super({ transform: true, whitelist: true, exceptionFactory: toException });
  }

  override async transform(
    value: unknown,
    metadata: ArgumentMetadata,
  ): Promise<unknown> {
    // ValidationPipe would validate a JSON array as an empty object; it is an invalid request format.
    if (metadata.type === 'body' && Array.isArray(value)) {
      throw new BadRequestException('Invalid request format');
    }
    return (await super.transform(value, metadata)) as unknown;
  }
}

function toException(errors: ValidationError[]): RequestValidationException {
  return new RequestValidationException(errors.flatMap(toFieldError));
}

// One error per field. class-validator does not report failed constraints in declaration order,
// so the most fundamental failure is picked explicitly: missing, then wrong type, then wrong value.
function toFieldError(error: ValidationError): FieldError[] {
  const constraints = Object.entries(error.constraints ?? {});
  const isTypeMismatch = (key: string) => {
    const context: unknown = error.contexts?.[key];
    return (
      typeof context === 'object' &&
      context !== null &&
      'typeMismatch' in context &&
      context.typeMismatch === TYPE_MISMATCH.typeMismatch
    );
  };

  const [key, message] =
    constraints.find(([k]) => k === 'isDefined') ??
    constraints.find(([k]) => isTypeMismatch(k)) ??
    constraints[0] ??
    [];
  if (key === undefined || message === undefined) {
    return [];
  }
  return [
    {
      field: error.property,
      message,
      code: isTypeMismatch(key) ? 'TYPE_MISMATCH' : null,
      rejectedValue: null,
    },
  ];
}
