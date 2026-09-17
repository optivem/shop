import { expect, type TestType } from '@playwright/test';
import type { MyShopDriver } from '../../../../../src/testkit/driver/port/my-shop-driver.js';

// Playwright's TestType is invariant in its fixture shape, so the helper is generic
// over the concrete fixtures and only requires `myShopDriver` to be among them.
export function runMyShopBaseSmokeTest<TTestArgs extends { myShopDriver: MyShopDriver }, TWorkerArgs extends object>(
  test: TestType<TTestArgs, TWorkerArgs>,
): void {
  test('shouldBeAbleToGoToMyShop', async ({ myShopDriver }) => {
    const result = await myShopDriver.goToMyShop({});
    expect(result.success).toBe(true);
  });
}
