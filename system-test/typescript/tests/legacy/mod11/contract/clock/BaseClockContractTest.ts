import type { ContractTest } from '../base/BaseExternalSystemContractTest.js';

export function registerClockContractTests(test: ContractTest): void {
  test('shouldBeAbleToGetTime', async ({ scenario }) => {
    await scenario.given().then().clock().hasTime();
  });
}
