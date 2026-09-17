import Decimal from 'decimal.js';
import { z } from 'zod';
import { FieldError } from './errors';

export type ValidationResult<T> = { ok: true; value: T } | { ok: false; errors: FieldError[] };

const DECIMAL_PATTERN = /^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?$/;

// Blank strings count as a missing value.
function blankAsMissing(value: unknown): unknown {
  return typeof value === 'string' && value.trim() === '' ? undefined : value;
}

// JSON clients may send numbers as numeric strings ("5"); anything else is left for the schema to reject.
function numericStringAsNumber(value: unknown): unknown {
  const present = blankAsMissing(value);
  return typeof present === 'string' ? Number(present) : present;
}

// Decimals are read from their text form, so they never pass through a JS float.
function numberAsString(value: unknown): unknown {
  const present = blankAsMissing(value);
  return typeof present === 'number' ? String(present) : present;
}

function requiredText(message: string) {
  return z.preprocess(blankAsMissing, z.string({ error: message }));
}

function optionalDateTime(message: string) {
  return z.preprocess(blankAsMissing, z.iso.datetime({ offset: true, error: message }).transform((s) => new Date(s)).nullish());
}

const placeOrderSchema = z.object({
  sku: requiredText('SKU must not be empty'),
  quantity: z.preprocess(
    numericStringAsNumber,
    z
      .number({ error: (issue) => (issue.input == null ? 'Quantity must not be empty' : 'Quantity must be an integer') })
      .int({ error: 'Quantity must be an integer' })
      .positive({ error: 'Quantity must be positive' })
  ),
  country: requiredText('Country must not be empty').transform((s) => s.trim()),
  couponCode: z.preprocess(blankAsMissing, z.string({ error: 'Coupon code must be a string' }).nullish()),
});

const publishCouponSchema = z.object({
  code: requiredText('Coupon code must not be blank').transform((s) => s.trim()),
  discountRate: z.preprocess(
    numberAsString,
    z
      .string({ error: (issue) => (issue.input == null ? 'Discount rate must not be null' : 'Discount rate must be a number') })
      .trim()
      .regex(DECIMAL_PATTERN, { error: 'Discount rate must be a number' })
      .transform((s) => new Decimal(s))
      .refine((rate) => rate.gt(0), { error: 'Discount rate must be greater than 0.00' })
      .refine((rate) => rate.lte(1), { error: 'Discount rate must be at most 1.00' })
  ),
  validFrom: optionalDateTime('Valid from must be a valid date-time'),
  validTo: optionalDateTime('Valid to must be a valid date-time'),
  usageLimit: z.preprocess(
    numericStringAsNumber,
    z
      .number({ error: 'Usage limit must be an integer' })
      .int({ error: 'Usage limit must be an integer' })
      .positive({ error: 'Usage limit must be positive' })
      .nullish()
  ),
});

export type PlaceOrderInput = z.infer<typeof placeOrderSchema>;
export type PublishCouponInput = z.infer<typeof publishCouponSchema>;

export function parsePlaceOrderRequest(body: unknown): ValidationResult<PlaceOrderInput> {
  return parse(placeOrderSchema, body);
}

export function parsePublishCouponRequest(body: unknown): ValidationResult<PublishCouponInput> {
  return parse(publishCouponSchema, body);
}

function parse<T>(schema: z.ZodType<T>, body: unknown): ValidationResult<T> {
  const result = schema.safeParse(body, { reportInput: true });
  if (result.success) {
    return { ok: true, value: result.data };
  }
  return { ok: false, errors: toFieldErrors(result.error.issues) };
}

// One error per field: the first failing rule, in declaration order.
function toFieldErrors(issues: z.core.$ZodIssue[]): FieldError[] {
  const errors = new Map<string, FieldError>();
  for (const issue of issues) {
    const field = issue.path.join('.');
    if (errors.has(field)) {
      continue;
    }
    const isTypeMismatch = (issue.code === 'invalid_type' || issue.code === 'invalid_format') && issue.input != null;
    errors.set(field, isTypeMismatch ? { field, message: issue.message, code: 'TYPE_MISMATCH' } : { field, message: issue.message });
  }
  return [...errors.values()];
}
