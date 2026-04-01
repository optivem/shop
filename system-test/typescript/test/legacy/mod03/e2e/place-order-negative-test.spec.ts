import { chromium, Browser } from 'playwright';
import { createScenario } from '../../../../src/test-setup';

describe('PlaceOrder Negative Test', () => {
  let browser: Browser;

  beforeAll(async () => {
    browser = await chromium.launch();
  });

  afterAll(async () => {
    await browser?.close();
  });

  const channels = ['api', 'ui'] as const;

  channels.forEach((channel) => {
    it(`shouldRejectOrderWithNonIntegerQuantity_${channel.toUpperCase()}`, async () => {
      const scenario = createScenario({ channel, externalSystemMode: 'real', browser });
      try {
        await scenario
          .when()
          .placeOrder()
          .withQuantity('3.5')
          .then()
          .shouldFail()
          .errorMessage('The request contains one or more validation errors')
          .fieldErrorMessage('quantity', 'Quantity must be an integer');
      } finally {
        await scenario.close();
      }
    });
  });
});
