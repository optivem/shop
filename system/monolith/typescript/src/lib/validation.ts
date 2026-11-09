import Decimal from 'decimal.js';
import { z } from 'zod';
import type { FieldError } from './errors';
import { isRecord } from './type-guards';

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

// Text stored as typed, minus surrounding spaces; blank (or only spaces) is missing.
function trimmedText(message: string) {
  return requiredText(message).transform((s) => s.trim());
}

export const PROFILE_COUNTRIES = ['US', 'DE', 'GB'] as const;
export const PROFILE_COUNTRY_UNSUPPORTED = 'Country must be US, DE or GB';

// The name and address fields shared by an order's shipping address and the customer profile.
const addressFields = {
  firstName: trimmedText('First name is required'),
  lastName: trimmedText('Last name is required'),
  street: trimmedText('Street is required'),
  postalCode: trimmedText('Postal code is required'),
  city: trimmedText('City is required'),
};

// Any non-empty country: it goes through the tax lookup, which reports an unknown one.
const shippingAddressSchema = z.object({
  ...addressFields,
  country: trimmedText('Country is required'),
});

const placeOrderSchema = z.object({
  sku: requiredText('SKU must not be empty'),
  quantity: z.preprocess(
    numericStringAsNumber,
    z
      .number({ error: (issue) => (issue.input == null ? 'Quantity must not be empty' : 'Quantity must be an integer') })
      .int({ error: 'Quantity must be an integer' })
      .positive({ error: 'Quantity must be positive' })
  ),
  // Required only without a shipping address: checked in parsePlaceOrderRequest.
  country: z.unknown().optional(),
  couponCode: z.preprocess(blankAsMissing, z.string({ error: 'Coupon code must be a string' }).nullish()),
  shippingAddress: z
    .object(shippingAddressSchema.shape, { error: 'Shipping address must be an object' })
    .nullish(),
});

const updateProfileSchema = z.object({
  ...addressFields,
  country: trimmedText('Country is required').pipe(
    z.enum(PROFILE_COUNTRIES, { error: PROFILE_COUNTRY_UNSUPPORTED })
  ),
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

type ParsedPlaceOrder = z.infer<typeof placeOrderSchema>;
export type ShippingAddressInput = z.infer<typeof shippingAddressSchema>;
export type UpdateProfileInput = z.infer<typeof updateProfileSchema>;

/** With a shipping address the tax country is the address's; without one the top-level country is required. */
export type PlaceOrderInput = Omit<ParsedPlaceOrder, 'country' | 'shippingAddress'> &
  (
    | { country: string; shippingAddress: null }
    | { country: null; shippingAddress: ShippingAddressInput }
  );
export type PublishCouponInput = z.infer<typeof publishCouponSchema>;

const quickOrderCountrySchema = z.object({
  country: requiredText('Country must not be empty').transform((s) => s.trim()),
});

function hasShippingAddress(body: unknown): boolean {
  return isRecord(body) && body.shippingAddress !== undefined && body.shippingAddress !== null;
}

export function parsePlaceOrderRequest(body: unknown): ValidationResult<PlaceOrderInput> {
  const result = parse(placeOrderSchema, body);
  // With a shipping address the top-level country is ignored; without one it is required.
  const country = hasShippingAddress(body) ? null : parse(quickOrderCountrySchema, body);
  const errors = [...(result.ok ? [] : result.errors)];
  if (country && !country.ok) {
    // Reported where the schema declares it: after sku and quantity.
    const index = errors.findIndex((e) => e.field !== 'sku' && e.field !== 'quantity');
    errors.splice(index === -1 ? errors.length : index, 0, ...country.errors);
  }
  if (!result.ok || (country && !country.ok) || errors.length > 0) {
    return { ok: false, errors };
  }
  const { shippingAddress, ...rest } = result.value;
  if (country) {
    return { ok: true, value: { ...rest, country: country.value.country, shippingAddress: null } };
  }
  return shippingAddress
    ? { ok: true, value: { ...rest, country: null, shippingAddress } }
    : { ok: false, errors };
}

export function parseUpdateProfileRequest(body: unknown): ValidationResult<UpdateProfileInput> {
  return parse(updateProfileSchema, body);
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
