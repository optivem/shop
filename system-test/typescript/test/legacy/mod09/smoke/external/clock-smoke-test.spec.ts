import { createScenario, ExternalSystemMode } from '../../../../../src/test-setup';

const externalSystemMode = (process.env.EXTERNAL_SYSTEM_MODE?.toLowerCase() || 'real') as ExternalSystemMode;

describe('Clock Smoke Test', () => {
  it('shouldBeAbleToGoToClock', async () => {
    const scenario = createScenario({ externalSystemMode });
    try {
      await scenario.assume().clock().shouldBeRunning();
    } finally {
      await scenario.close();
    }
  });
});
