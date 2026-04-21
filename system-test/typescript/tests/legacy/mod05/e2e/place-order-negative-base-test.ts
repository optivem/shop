import { expect, type TestType } from '@playwright/test';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function runPlaceOrderNegative(test: TestType<any, any>): void {
  test('shouldRejectOrderWithNonIntegerQuantity', async ({ shopDriver }) => {
    const result = await shopDriver.placeOrder({ sku: 'SOME-SKU', quantity: '3.5', country: 'US' });

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.message).toContain('The request contains one or more validation errors');
      const quantityError = result.error.fieldErrors.find((e: { field: string; message: string }) => e.field === 'quantity');
      expect(quantityError?.message).toBe('Quantity must be an integer');
    }
  });
}
