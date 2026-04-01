import { chromium, Browser } from 'playwright';
import { createScenario, Channel } from '../../../../src/test-setup';

const channel = (process.env.CHANNEL?.toLowerCase() || 'api') as Channel;

describe('PlaceOrder Negative Test', () => {
  let browser: Browser;

  beforeAll(async () => {
    if (channel === 'ui') {
      browser = await chromium.launch();
    }
  });

  afterAll(async () => {
    await browser?.close();
  });

  it('shouldRejectOrderWithNonIntegerQuantity', async () => {
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
