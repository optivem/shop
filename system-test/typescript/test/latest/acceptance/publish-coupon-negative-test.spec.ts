import { createScenario, Channel, ExternalSystemMode } from '../../../src/test-setup';

const channel = (process.env.CHANNEL?.toLowerCase() || 'api') as Channel;
const externalSystemMode = (process.env.EXTERNAL_SYSTEM_MODE?.toLowerCase() || 'stub') as ExternalSystemMode;

describe('PublishCoupon Negative Test', () => {
  const emptyCodes = ['', '   '];
  const nonPositiveDiscountRates = [0.0, -0.1];
  const aboveOneDiscountRates = [1.01, 2.0];

  emptyCodes.forEach((code) => {
    it(`shouldRejectCouponWithBlankCode_${channel.toUpperCase()}_"${code}"`, async () => {
      const scenario = createScenario({ channel, externalSystemMode });
      try {
        await scenario
          .when()
          .publishCoupon()
          .withCode(code)
          .withDiscountRate(0.1)
          .then()
          .shouldFail()
          .errorMessage('The request contains one or more validation errors')
          .fieldErrorMessage('code', 'Coupon code must not be blank');
      } finally {
        await scenario.close();
      }
    });
  });

  nonPositiveDiscountRates.forEach((discountRate) => {
    it(`shouldRejectCouponWithNonPositiveDiscountRate_${channel.toUpperCase()}_${discountRate}`, async () => {
      const scenario = createScenario({ channel, externalSystemMode });
      try {
        await scenario
          .when()
          .publishCoupon()
          .withCode('INVALID')
          .withDiscountRate(discountRate)
          .then()
          .shouldFail()
          .errorMessage('The request contains one or more validation errors')
          .fieldErrorMessage('discountRate', 'Discount rate must be greater than 0.00');
      } finally {
        await scenario.close();
      }
    });
  });

  aboveOneDiscountRates.forEach((discountRate) => {
    it(`shouldRejectCouponWithDiscountRateAboveOne_${channel.toUpperCase()}_${discountRate}`, async () => {
      const scenario = createScenario({ channel, externalSystemMode });
      try {
        await scenario
          .when()
          .publishCoupon()
          .withCode('INVALID')
          .withDiscountRate(discountRate)
          .then()
          .shouldFail()
          .errorMessage('The request contains one or more validation errors')
          .fieldErrorMessage('discountRate', 'Discount rate must be at most 1.00');
      } finally {
        await scenario.close();
      }
    });
  });
});
