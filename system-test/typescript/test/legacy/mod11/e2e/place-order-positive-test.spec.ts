import { chromium, Browser } from 'playwright';
import { createScenario, Channel } from '../../../../src/test-setup';

const channel = (process.env.CHANNEL?.toLowerCase() || 'api') as Channel;

describe('PlaceOrder Positive Test', () => {
  let browser: Browser;

  beforeAll(async () => {
    if (channel === 'ui') {
      browser = await chromium.launch();
    }
  });

  afterAll(async () => {
    await browser?.close();
  });

  it('shouldPlaceOrder', async () => {
    const scenario = createScenario({ channel, externalSystemMode: 'real', browser });
    try {
      await scenario
        .when()
        .placeOrder()
        .then()
        .shouldSucceed();
    } finally {
      await scenario.close();
    }
  });
});
