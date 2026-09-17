import Decimal from 'decimal.js';
import { parsePlaceOrderRequest, parsePublishCouponRequest } from '../lib/validation';

const validOrder = { sku: 'BOOK-123', quantity: 2, country: 'US' };
const validCoupon = { code: 'SAVE10', discountRate: '0.10' };

describe('parsePlaceOrderRequest', () => {
  it('accepts numeric strings and trims the country', () => {
    expect(parsePlaceOrderRequest({ ...validOrder, quantity: '5', country: ' US ' })).toEqual({
      ok: true,
      value: { sku: 'BOOK-123', quantity: 5, country: 'US' },
    });
  });

  it('treats a blank coupon code as absent', () => {
    const result = parsePlaceOrderRequest({ ...validOrder, couponCode: '  ' });
    expect(result).toEqual({ ok: true, value: validOrder });
  });

  it.each([undefined, null, '', '   '])('reports an empty quantity (%p)', (quantity) => {
    expect(parsePlaceOrderRequest({ ...validOrder, quantity })).toEqual({
      ok: false,
      errors: [{ field: 'quantity', message: 'Quantity must not be empty' }],
    });
  });

  it.each(['3.5', 'lala', 3.5, true, {}])('reports a non-integer quantity as a type mismatch (%p)', (quantity) => {
    expect(parsePlaceOrderRequest({ ...validOrder, quantity })).toEqual({
      ok: false,
      errors: [{ field: 'quantity', message: 'Quantity must be an integer', code: 'TYPE_MISMATCH' }],
    });
  });

  it.each([0, -10, '-1'])('reports a non-positive quantity (%p)', (quantity) => {
    expect(parsePlaceOrderRequest({ ...validOrder, quantity })).toEqual({
      ok: false,
      errors: [{ field: 'quantity', message: 'Quantity must be positive' }],
    });
  });

  it('reports every invalid field, one error each', () => {
    expect(parsePlaceOrderRequest({ sku: '', quantity: null, country: '  ', couponCode: 7 })).toEqual({
      ok: false,
      errors: [
        { field: 'sku', message: 'SKU must not be empty' },
        { field: 'quantity', message: 'Quantity must not be empty' },
        { field: 'country', message: 'Country must not be empty' },
        { field: 'couponCode', message: 'Coupon code must be a string', code: 'TYPE_MISMATCH' },
      ],
    });
  });
});

describe('parsePublishCouponRequest', () => {
  it('parses the discount rate exactly and optional fields to typed values', () => {
    const result = parsePublishCouponRequest({
      code: ' SAVE10 ',
      discountRate: '0.15',
      validFrom: '2024-06-01T00:00:00Z',
      validTo: '',
      usageLimit: '3',
    });

    expect(result.ok && result.value).toEqual({
      code: 'SAVE10',
      discountRate: new Decimal('0.15'),
      validFrom: new Date('2024-06-01T00:00:00Z'),
      validTo: undefined,
      usageLimit: 3,
    });
  });

  it.each([undefined, null, ''])('reports a missing discount rate (%p)', (discountRate) => {
    expect(parsePublishCouponRequest({ ...validCoupon, discountRate })).toEqual({
      ok: false,
      errors: [{ field: 'discountRate', message: 'Discount rate must not be null' }],
    });
  });

  it.each(['abc', true, 'NaN', 'Infinity'])('reports a non-numeric discount rate as a type mismatch (%p)', (discountRate) => {
    expect(parsePublishCouponRequest({ ...validCoupon, discountRate })).toEqual({
      ok: false,
      errors: [{ field: 'discountRate', message: 'Discount rate must be a number', code: 'TYPE_MISMATCH' }],
    });
  });

  it.each(['0.0', '-0.01', 0])('reports a zero or negative discount rate (%p)', (discountRate) => {
    expect(parsePublishCouponRequest({ ...validCoupon, discountRate })).toEqual({
      ok: false,
      errors: [{ field: 'discountRate', message: 'Discount rate must be greater than 0.00' }],
    });
  });

  it.each(['1.01', 2])('reports a discount rate above 1 (%p)', (discountRate) => {
    expect(parsePublishCouponRequest({ ...validCoupon, discountRate })).toEqual({
      ok: false,
      errors: [{ field: 'discountRate', message: 'Discount rate must be at most 1.00' }],
    });
  });

  it.each(['  ', 42])('reports a blank or non-string code (%p)', (code) => {
    const result = parsePublishCouponRequest({ ...validCoupon, code });
    expect(result.ok).toBe(false);
    expect(!result.ok && result.errors.map((e) => e.message)).toEqual(['Coupon code must not be blank']);
  });

  it('reports an invalid date as a type mismatch', () => {
    expect(parsePublishCouponRequest({ ...validCoupon, validFrom: 'not-a-date' })).toEqual({
      ok: false,
      errors: [{ field: 'validFrom', message: 'Valid from must be a valid date-time', code: 'TYPE_MISMATCH' }],
    });
  });

  it.each(['0', '-1', 2.5, 'x'])('reports an invalid usage limit (%p)', (usageLimit) => {
    const result = parsePublishCouponRequest({ ...validCoupon, usageLimit });
    expect(result.ok).toBe(false);
    expect(!result.ok && result.errors[0]?.field).toBe('usageLimit');
  });
});
