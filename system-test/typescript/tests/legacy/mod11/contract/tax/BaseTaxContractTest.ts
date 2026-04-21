import type { ContractTest } from '../base/BaseExternalSystemContractTest.js';

export function registerTaxContractTests(test: ContractTest): void {
  test('shouldBeAbleToGetTaxRate', async ({ scenario }) => {
    await scenario
      .given()
      .country()
      .withCode('US')
      .withTaxRate(0.09)
      .then()
      .country('US')
      .hasTaxRateIsPositive();
  });
}
