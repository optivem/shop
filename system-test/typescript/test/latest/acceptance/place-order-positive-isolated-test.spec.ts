import { chromium, Browser } from 'playwright';
import { createScenario, Channel, ExternalSystemMode } from '../../../src/test-setup';

const channel = (process.env.CHANNEL?.toLowerCase() || 'api') as Channel;
const externalSystemMode = (process.env.EXTERNAL_SYSTEM_MODE?.toLowerCase() || 'stub') as ExternalSystemMode;

describe('PlaceOrder Positive Isolated Test', () => {
  let browser: Browser;

  beforeAll(async () => {
    if (channel === 'ui') {
      browser = await chromium.launch();
    }
  });

  afterAll(async () => {
    await browser?.close();
  });

  it(`shouldRecordPlacementTimestamp_${channel.toUpperCase()}`, async () => {
    const scenario = createScenario({ channel, externalSystemMode, browser });
    try {
      await scenario
        .given()
        .clock()
        .withTime('2026-01-15T10:30:00Z')
        .when()
        .placeOrder()
        .then()
        .shouldSucceed()
        .and()
        .clock()
        .hasTime('2026-01-15T10:30:00Z');
    } finally {
      await scenario.close();
    }
  });

  it(`shouldApplyFullPriceOnWeekday_${channel.toUpperCase()}`, async () => {
    const scenario = createScenario({ channel, externalSystemMode, browser });
    try {
      await scenario
        .given()
        .product()
        .withUnitPrice(20.0)
        .and()
        .clock()
        .withWeekday()
        .when()
        .placeOrder()
        .withQuantity(5)
        .then()
        .shouldSucceed()
        .and()
        .order()
        .hasTotalPrice(100.0);
    } finally {
      await scenario.close();
    }
  });

  it(`shouldApplyWeekendDiscount_${channel.toUpperCase()}`, async () => {
    const scenario = createScenario({ channel, externalSystemMode, browser });
    try {
      await scenario
        .given()
        .product()
        .withUnitPrice(20.0)
        .and()
        .clock()
        .withWeekend()
        .when()
        .placeOrder()
        .withQuantity(5)
        .then()
        .shouldSucceed()
        .and()
        .order()
        .hasTotalPrice(50.0);
    } finally {
      await scenario.close();
    }
  });
});
