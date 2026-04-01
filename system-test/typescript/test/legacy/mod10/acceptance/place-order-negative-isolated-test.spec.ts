import { chromium, Browser } from 'playwright';
import { createScenario } from '../../../../src/test-setup';

describe('PlaceOrder Negative Isolated Test', () => {
  let browser: Browser;

  beforeAll(async () => {
    browser = await chromium.launch();
  });

  afterAll(async () => {
    await browser?.close();
  });

  const channels = ['api', 'ui'] as const;

  channels.forEach((channel) => {
    it(`shouldRejectOrderPlacedAtYearEnd_${channel.toUpperCase()}`, async () => {
      const scenario = createScenario({ channel, externalSystemMode: 'stub', browser });
      try {
        await scenario
          .given()
          .clock()
          .withTime('2026-12-31T23:59:30Z')
          .when()
          .placeOrder()
          .then()
          .shouldFail()
          .errorMessage('Orders cannot be placed between 23:59 and 00:00 on December 31st');
      } finally {
        await scenario.close();
      }
    });
  });
});
