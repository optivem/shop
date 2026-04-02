import { chromium, Browser } from 'playwright';
import { createScenario, Channel } from '../../../../src/test-setup';
import { OrderStatus } from '../../../../src/common/dtos';

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

  it('shouldPlaceOrderForValidInput', async () => {
    const scenario = createScenario({ channel, externalSystemMode: 'real', browser });
    try {
      await scenario
        .given()
        .product()
        .withUnitPrice(20.0)
        .when()
        .placeOrder()
        .withQuantity(5)
        .then()
        .shouldSucceed()
        .and()
        .order()
        .hasOrderNumberPrefix('ORD-')
        .hasStatus(OrderStatus.PLACED);
    } finally {
      await scenario.close();
    }
  });
});
