import { createScenario, ExternalSystemMode } from '../../../../../src/test-setup';

const externalSystemMode = (process.env.EXTERNAL_SYSTEM_MODE?.toLowerCase() || 'real') as ExternalSystemMode;

describe('ERP Smoke Test', () => {
  it('shouldBeAbleToGoToErp', async () => {
    const scenario = createScenario({ externalSystemMode });
    try {
      await scenario.assume().erp().shouldBeRunning();
    } finally {
      await scenario.close();
    }
  });
});
