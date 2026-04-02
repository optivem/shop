import { chromium, Browser } from 'playwright';
import { createScenario, Channel, ExternalSystemMode } from '../../../../src/test-setup';
import { OrderStatus } from '../../../../src/common/dtos';

const channel = (process.env.CHANNEL?.toLowerCase() || 'api') as Channel;
const externalSystemMode = (process.env.EXTERNAL_SYSTEM_MODE?.toLowerCase() || 'stub') as ExternalSystemMode;

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

  it('orderNumberShouldStartWithORD', async () => {
    const scenario = createScenario({ channel, externalSystemMode, browser });
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

  it('orderStatusShouldBePlacedAfterPlacingOrder', async () => {
    const scenario = createScenario({ channel, externalSystemMode, browser });
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
