import { chromium, Browser } from 'playwright';
import { createScenario } from '../../../src/test-setup';
import { OrderStatus } from '../../../src/common/dtos';

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
    it(`orderNumberShouldStartWithORD_${channel.toUpperCase()}`, async () => {
      const scenario = createScenario({ channel, externalSystemMode: 'stub', browser });
      try {
        await scenario
          .when()
          .placeOrder()
          .then()
          .shouldSucceed()
          .and()
          .order()
          .hasOrderNumberPrefix('ORD-');
      } finally {
        await scenario.close();
      }
    });

    it(`orderStatusShouldBePlacedAfterPlacingOrder_${channel.toUpperCase()}`, async () => {
      const scenario = createScenario({ channel, externalSystemMode: 'stub', browser });
      try {
        await scenario
          .when()
          .placeOrder()
          .then()
          .shouldSucceed()
          .and()
          .order()
          .hasStatus(OrderStatus.PLACED);
      } finally {
        await scenario.close();
      }
    });
  });
});
