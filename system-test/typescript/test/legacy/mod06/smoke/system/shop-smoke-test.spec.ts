import { chromium, Browser } from 'playwright';
import { createScenario } from '../../../../../src/test-setup';

describe('Shop Smoke Test', () => {
  let browser: Browser;

  beforeAll(async () => {
    browser = await chromium.launch();
  });

  afterAll(async () => {
    await browser?.close();
  });

  const channels = ['api', 'ui'] as const;

  channels.forEach((channel) => {
    it(`shouldBeAbleToGoToShop_${channel.toUpperCase()}`, async () => {
      const scenario = createScenario({ channel, browser });
      try {
        await scenario.assume().shop().shouldBeRunning();
      } finally {
        await scenario.close();
      }
    });
  });
});
