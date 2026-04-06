import { chromium, Browser } from 'playwright';
import { createScenario, Channel, ExternalSystemMode } from '../../../src/test-setup';
import { OrderStatus } from '../../../src/common/dtos';

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

  it(`orderNumberShouldStartWithORD_${channel.toUpperCase()}`, async () => {
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

  it(`orderStatusShouldBePlacedAfterPlacingOrder_${channel.toUpperCase()}`, async () => {
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

  it(`orderTotalShouldIncludeTax_API`, async () => {
    if (channel !== 'api') return;
    const scenario = createScenario({ channel: 'api', externalSystemMode });
    try {
      await scenario
        .when()
        .placeOrder()
        .withCountry('DE')
        .then()
        .shouldSucceed()
        .and()
        .order()
        .hasSubtotalPrice(20.0)
        .hasTaxRate(0.19)
        .hasTotalPrice(23.8);
    } finally {
      await scenario.close();
    }
  });

  it(`orderTotalShouldReflectCouponDiscount_API`, async () => {
    if (channel !== 'api') return;
    const scenario = createScenario({ channel: 'api', externalSystemMode });
    try {
      await scenario
        .given()
        .coupon()
        .withCode('DISC10')
        .withDiscountRate(0.1)
        .when()
        .placeOrder()
        .withCouponCode('DISC10')
        .then()
        .shouldSucceed()
        .and()
        .order()
        .hasSubtotalPrice(18.0)
        .hasDiscountRate(0.1)
        .hasAppliedCouponCode('DISC10')
        .hasTotalPrice(18.0);
    } finally {
      await scenario.close();
    }
  });

  it(`orderTotalShouldApplyCouponDiscountAndTax_API`, async () => {
    if (channel !== 'api') return;
    const scenario = createScenario({ channel: 'api', externalSystemMode });
    try {
      await scenario
        .given()
        .coupon()
        .withCode('COMBO10')
        .withDiscountRate(0.1)
        .when()
        .placeOrder()
        .withCountry('GB')
        .withCouponCode('COMBO10')
        .then()
        .shouldSucceed()
        .and()
        .order()
        .hasSubtotalPrice(18.0)
        .hasDiscountRate(0.1)
        .hasTaxRate(0.2)
        .hasAppliedCouponCode('COMBO10')
        .hasTotalPrice(21.6);
    } finally {
      await scenario.close();
    }
  });
});
