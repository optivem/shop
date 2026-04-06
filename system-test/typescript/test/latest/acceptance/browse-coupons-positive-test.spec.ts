import { createScenario, Channel, ExternalSystemMode } from '../../../src/test-setup';

const channel = (process.env.CHANNEL?.toLowerCase() || 'api') as Channel;
const externalSystemMode = (process.env.EXTERNAL_SYSTEM_MODE?.toLowerCase() || 'stub') as ExternalSystemMode;

describe('BrowseCoupons Positive Test', () => {
  it(`publishedCouponShouldAppearInList_${channel.toUpperCase()}`, async () => {
    const scenario = createScenario({ channel, externalSystemMode });
    try {
      await scenario
        .given()
        .coupon()
        .withCode('BROWSE10')
        .withDiscountRate(0.1)
        .when()
        .browseCoupons()
        .then()
        .shouldSucceed()
        .coupons()
        .containsCouponWithCode('BROWSE10');
    } finally {
      await scenario.close();
    }
  });
});
