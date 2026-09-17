import Decimal from 'decimal.js';
import { Transform } from 'class-transformer';
import { isISO8601, ValidateBy, ValidationOptions } from 'class-validator';

// Marks a constraint whose failure means the value has the wrong type, not a wrong value.
export const TYPE_MISMATCH = { typeMismatch: true };

const DECIMAL_PATTERN = /^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?$/;

function blankAsMissing(value: unknown): unknown {
  return typeof value === 'string' && value.trim() === '' ? undefined : value;
}

// Blank strings count as a missing value.
export function BlankAsMissing(): PropertyDecorator {
  return Transform(({ value }) => blankAsMissing(value));
}

// JSON clients may send numbers as numeric strings ("5"); anything else is left for the validators to reject.
export function NumericStringAsNumber(): PropertyDecorator {
  return Transform(({ value }) => {
    const present = blankAsMissing(value);
    return typeof present === 'string' ? Number(present) : present;
  });
}

// Decimals are read from their text form, so they never pass through a JS float.
export function ToDecimal(): PropertyDecorator {
  return Transform(({ value }) => {
    const present = blankAsMissing(value);
    const text = typeof present === 'number' ? String(present) : present;
    return typeof text === 'string' && DECIMAL_PATTERN.test(text.trim())
      ? new Decimal(text.trim())
      : present;
  });
}

export function ToDateTime(): PropertyDecorator {
  return Transform(({ value }) => {
    const present = blankAsMissing(value);
    return typeof present === 'string' && isISO8601(present, { strict: true })
      ? new Date(present)
      : present;
  });
}

export function IsDecimal(options: ValidationOptions): PropertyDecorator {
  return ValidateBy(
    {
      name: 'isDecimal',
      validator: { validate: (value) => Decimal.isDecimal(value) },
    },
    options,
  );
}

export function IsDecimalGreaterThan(
  min: number,
  options: ValidationOptions,
): PropertyDecorator {
  return ValidateBy(
    {
      name: 'isDecimalGreaterThan',
      validator: {
        validate: (value) => Decimal.isDecimal(value) && value.gt(min),
      },
    },
    options,
  );
}

export function IsDecimalAtMost(
  max: number,
  options: ValidationOptions,
): PropertyDecorator {
  return ValidateBy(
    {
      name: 'isDecimalAtMost',
      validator: {
        validate: (value) => Decimal.isDecimal(value) && value.lte(max),
      },
    },
    options,
  );
}
