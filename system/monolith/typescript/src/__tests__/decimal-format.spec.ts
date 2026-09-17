import Decimal from 'decimal.js';
import { jsonResponseWithDecimals } from '../lib/decimal-format';

async function bodyOf(data: unknown): Promise<string> {
  return jsonResponseWithDecimals(data).text();
}

describe('jsonResponseWithDecimals', () => {
  it('writes money fields as JSON numbers with two decimals, rounding half up', async () => {
    const body = await bodyOf({ unitPrice: new Decimal('20'), totalPrice: new Decimal('10.005') });
    expect(body).toBe('{"unitPrice":20.00,"totalPrice":10.01}');
  });

  it('writes rate fields with four decimals', async () => {
    const body = await bodyOf({ discountRate: new Decimal('0.1'), taxRate: new Decimal('0.07255') });
    expect(body).toBe('{"discountRate":0.1000,"taxRate":0.0726}');
  });

  it('formats from the exact Decimal, not a float', async () => {
    const body = await bodyOf({ totalPrice: new Decimal('0.1').plus('0.2') });
    expect(body).toBe('{"totalPrice":0.30}');
  });

  it('keeps the sign of negative values', async () => {
    const body = await bodyOf({ discountAmount: new Decimal('-1.5') });
    expect(body).toBe('{"discountAmount":-1.50}');
  });

  it('formats decimal fields inside nested objects and arrays', async () => {
    const body = await bodyOf({ orders: [{ orderNumber: 'ORD-1', totalPrice: new Decimal('5') }] });
    expect(body).toBe('{"orders":[{"orderNumber":"ORD-1","totalPrice":5.00}]}');
  });

  it('leaves non-decimal fields and non-Decimal values untouched', async () => {
    const body = await bodyOf({ quantity: 3, unitPrice: null, appliedCouponCode: 'SAVE10' });
    expect(body).toBe('{"quantity":3,"unitPrice":null,"appliedCouponCode":"SAVE10"}');
  });

  it('sets the status, JSON content type and extra headers', () => {
    const response = jsonResponseWithDecimals({}, { status: 201, headers: { Location: '/api/orders/ORD-1' } });
    expect(response.status).toBe(201);
    expect(response.headers.get('Content-Type')).toBe('application/json; charset=utf-8');
    expect(response.headers.get('Location')).toBe('/api/orders/ORD-1');
  });

  it('defaults the status to 200', () => {
    expect(jsonResponseWithDecimals({}).status).toBe(200);
  });
});
