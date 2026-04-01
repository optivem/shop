import { chromium, Browser } from 'playwright';
import { createScenario } from '../../../src/test-setup';
import { ScenarioDsl } from '../../../src/dsl/scenario-dsl';

describe('PlaceOrder Positive Test', () => {
  let browser: Browser;

  beforeAll(async () => {
    browser = await chromium.launch();
  });

  afterAll(async () => {
    await browser?.close();
  });

  const channels = ['api', 'ui'] as const;

  channels.forEach((channel) => {
    it(`shouldPlaceOrder_${channel.toUpperCase()}`, async () => {
      const scenario = createScenario({ channel, externalSystemMode: 'real', browser });
      try {
        await scenario.when().placeOrder().then().shouldSucceed();
      } finally {
        await scenario.close();
      }
    });
  });
});
