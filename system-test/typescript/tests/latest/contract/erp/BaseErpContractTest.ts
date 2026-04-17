import type { ContractTest } from '../base/BaseExternalSystemContractTest.js';

export function registerErpContractTests(test: ContractTest): void {
  test('shouldBeAbleToGetProduct', async ({ scenario }) => {
    await scenario
      .given()
      .product()
      .withSku('SKU-123')
      .withUnitPrice(12.0)
      .then()
      .product('SKU-123')
      .hasSku('SKU-123')
      .hasPrice(12.0);
  });
}
