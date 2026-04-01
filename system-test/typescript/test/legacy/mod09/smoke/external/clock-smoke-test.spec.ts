import { createScenario } from '../../../../../src/test-setup';

describe('Clock Smoke Test', () => {
  it('shouldBeAbleToGoToClock', async () => {
    const scenario = createScenario();
    try {
      await scenario.assume().clock().shouldBeRunning();
    } finally {
      await scenario.close();
    }
  });
});
