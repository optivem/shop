import { createScenario } from '../../../../../src/test-setup';

describe('ERP Smoke Test', () => {
  it('shouldBeAbleToGoToErp', async () => {
    const scenario = createScenario();
    try {
      await scenario.assume().erp().shouldBeRunning();
    } finally {
      await scenario.close();
    }
  });
});
