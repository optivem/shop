import { ArgumentMetadata, BadRequestException, Injectable, PipeTransform } from '@nestjs/common';
import { validate } from 'class-validator';
import { plainToInstance } from 'class-transformer';

// Metadata for fields that need type mismatch detection
const TYPE_VALIDATION_METADATA: Record<string, Record<string, { expectedType: string; message: string }>> = {
  PlaceOrderRequest: {
    quantity: { expectedType: 'integer', message: 'Quantity must be an integer' },
  },
};

@Injectable()
export class CustomValidationPipe implements PipeTransform {
  async transform(value: unknown, metadata: ArgumentMetadata) {
    if (!metadata.metatype || !this.toValidate(metadata)) {
      return value;
    }

    const rawBody = typeof value === 'object' && value !== null ? { ...value } as Record<string, unknown> : {};
    const className = metadata.metatype.name;

    const object = plainToInstance(metadata.metatype, value);
    const errors = await validate(object as object);

    if (errors.length > 0) {
      const fieldMeta = TYPE_VALIDATION_METADATA[className] || {};

      const validationErrors = errors.map((err) => {
        const field = err.property;
        const constraints = err.constraints || {};
        const meta = fieldMeta[field];

        return {
          field,
          constraints,
          expectedType: meta?.expectedType,
          typeMismatchMessage: meta?.message,
        };
      });

      throw new BadRequestException({
        validationErrors,
        rawBody,
      });
    }

    return object;
  }

  private toValidate(metadata: ArgumentMetadata): boolean {
    const types: Array<new (...args: unknown[]) => unknown> = [String, Boolean, Number, Array, Object];
    return !types.includes(metadata.metatype as new (...args: unknown[]) => unknown);
  }
}
