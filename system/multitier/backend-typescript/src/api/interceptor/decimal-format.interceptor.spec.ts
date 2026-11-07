import type { CallHandler, ExecutionContext } from '@nestjs/common';
import Decimal from 'decimal.js';
import { lastValueFrom, of } from 'rxjs';
import { DecimalFormatInterceptor } from './decimal-format.interceptor';

async function bodyOf(
  data: unknown,
): Promise<{ body: unknown; headers: Record<string, string> }> {
  const headers: Record<string, string> = {};
  const context = {
    switchToHttp: () => ({
      getResponse: () => ({
        setHeader: (name: string, value: string) => {
          headers[name] = value;
        },
      }),
    }),
  } as unknown as ExecutionContext;
  const next: CallHandler = { handle: () => of(data) };
  const body = await lastValueFrom(
    new DecimalFormatInterceptor().intercept(context, next),
  );
  return { body, headers };
}

describe('DecimalFormatInterceptor', () => {
  it('writes money fields as JSON strings with two decimals, rounding half up', async () => {
    const { body } = await bodyOf({
      unitPrice: new Decimal('20'),
      totalPrice: new Decimal('10.005'),
    });
    expect(body).toBe('{"unitPrice":"20.00","totalPrice":"10.01"}');
  });

  it('writes a catalog product price as a JSON string with two decimals', async () => {
    const { body } = await bodyOf([
      { sku: 'BOOK-123', price: new Decimal('12') },
      { sku: 'BOOK-456', price: 15.5 },
    ]);
    expect(body).toBe(
      '[{"sku":"BOOK-123","price":"12.00"},{"sku":"BOOK-456","price":"15.50"}]',
    );
  });

  it('writes rate fields as JSON strings with four decimals', async () => {
    const { body } = await bodyOf({
      discountRate: new Decimal('0.2'),
      taxRate: new Decimal('0.07255'),
    });
    expect(body).toBe('{"discountRate":"0.2000","taxRate":"0.0726"}');
  });

  it('formats from the exact Decimal, not a float', async () => {
    const { body } = await bodyOf({
      totalPrice: new Decimal('0.1').plus('0.2'),
    });
    expect(body).toBe('{"totalPrice":"0.30"}');
  });

  it('keeps the sign of negative values', async () => {
    const { body } = await bodyOf({ discountAmount: new Decimal('-1.5') });
    expect(body).toBe('{"discountAmount":"-1.50"}');
  });

  it('formats decimal fields inside nested objects and arrays', async () => {
    const { body } = await bodyOf({
      orders: [{ orderNumber: 'ORD-1', totalPrice: new Decimal('5') }],
    });
    expect(body).toBe(
      '{"orders":[{"orderNumber":"ORD-1","totalPrice":"5.00"}]}',
    );
  });

  it('leaves non-decimal fields and null values untouched', async () => {
    const { body } = await bodyOf({
      quantity: 3,
      unitPrice: null,
      appliedCouponCode: 'SAVE10',
    });
    expect(body).toBe(
      '{"quantity":3,"unitPrice":null,"appliedCouponCode":"SAVE10"}',
    );
  });

  it('sets the JSON content type', async () => {
    const { headers } = await bodyOf({});
    expect(headers['Content-Type']).toBe('application/json; charset=utf-8');
  });

  it('passes an empty body through', async () => {
    const { body } = await bodyOf(undefined);
    expect(body).toBeUndefined();
  });
});
