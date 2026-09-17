import { expect, type TestType } from '@playwright/test';
import { assertThatResult } from '../../../../src/testkit/common/result-assert.js';
import type { MyShopDriver } from '../../../../src/testkit/driver/port/my-shop-driver.js';

export function runPlaceOrderNegative<TTestArgs extends { myShopDriver: MyShopDriver }, TWorkerArgs extends object>(
  test: TestType<TTestArgs, TWorkerArgs>,
): void {
  test('shouldRejectOrderWithNonIntegerQuantity', async ({ myShopDriver }) => {
    const result = await myShopDriver.placeOrder({ sku: 'SOME-SKU', quantity: '3.5', country: 'US' });

    expect(result.success).toBe(false);
    const error = assertThatResult(result).getError();
    expect(error.message).toContain('The request contains one or more validation errors');
    const quantityError = error.fieldErrors.find((e: { field: string; message: string }) => e.field === 'quantity');
    expect(quantityError?.message).toBe('Quantity must be an integer');
  });
}
