import { createScenario, Channel, ExternalSystemMode } from '../../../src/test-setup';

const channel = (process.env.CHANNEL?.toLowerCase() || 'api') as Channel;
const externalSystemMode = (process.env.EXTERNAL_SYSTEM_MODE?.toLowerCase() || 'stub') as ExternalSystemMode;

describe('PublishCoupon Positive Test', () => {
  it(`shouldPublishCouponSuccessfully_${channel.toUpperCase()}`, async () => {
    const scenario = createScenario({ channel, externalSystemMode });
    try {
      await scenario
        .when()
        .publishCoupon()
        .withCode('SAVE10')
        .withDiscountRate(0.1)
        .then()
        .shouldSucceed();
    } finally {
      await scenario.close();
    }
  });
});
