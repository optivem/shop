import { chromium, Browser } from 'playwright';
import { createScenario } from '../../../src/test-setup';

describe('PlaceOrder Negative Test', () => {
  let browser: Browser;

  beforeAll(async () => {
    browser = await chromium.launch();
  });

  afterAll(async () => {
    await browser?.close();
  });

  const channels = ['api', 'ui'] as const;
  const nonIntegerQuantities = ['3.5', 'lala', 'invalid-quantity'];
  const emptySkus = ['', '   '];
  const nonPositiveQuantities = ['-10', '-1', '0'];
  const emptyQuantities = ['', '   '];

  channels.forEach((channel) => {
    nonIntegerQuantities.forEach((qty) => {
      it(`shouldRejectOrderWithNonIntegerQuantity_${channel.toUpperCase()}_${qty}`, async () => {
        const scenario = createScenario({ channel, externalSystemMode: 'stub', browser });
        try {
          await scenario
            .when()
            .placeOrder()
            .withQuantity(qty)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must be an integer');
        } finally {
          await scenario.close();
        }
      });
    });

    it(`shouldRejectOrderForNonExistentProduct_${channel.toUpperCase()}`, async () => {
      const scenario = createScenario({ channel, externalSystemMode: 'stub', browser });
      try {
        await scenario
          .when()
          .placeOrder()
          .withSku('NON-EXISTENT-SKU-12345')
          .withQuantity(1)
          .then()
          .shouldFail()
          .errorMessage('The request contains one or more validation errors')
          .fieldErrorMessage('sku', 'Product does not exist for SKU: NON-EXISTENT-SKU-12345');
      } finally {
        await scenario.close();
      }
    });

    emptySkus.forEach((sku) => {
      it(`shouldRejectOrderWithEmptySku_${channel.toUpperCase()}_"${sku}"`, async () => {
        const scenario = createScenario({ channel, externalSystemMode: 'stub', browser });
        try {
          await scenario
            .when()
            .placeOrder()
            .withSku(sku)
            .withQuantity(1)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('sku', 'SKU must not be empty');
        } finally {
          await scenario.close();
        }
      });
    });

    nonPositiveQuantities.forEach((qty) => {
      it(`shouldRejectOrderWithNonPositiveQuantity_${channel.toUpperCase()}_${qty}`, async () => {
        const scenario = createScenario({ channel, externalSystemMode: 'stub', browser });
        try {
          await scenario
            .when()
            .placeOrder()
            .withQuantity(qty)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must be positive');
        } finally {
          await scenario.close();
        }
      });
    });

    emptyQuantities.forEach((qty) => {
      it(`shouldRejectOrderWithEmptyQuantity_${channel.toUpperCase()}_"${qty}"`, async () => {
        const scenario = createScenario({ channel, externalSystemMode: 'stub', browser });
        try {
          await scenario
            .when()
            .placeOrder()
            .withQuantity(qty)
            .then()
            .shouldFail()
            .errorMessage('The request contains one or more validation errors')
            .fieldErrorMessage('quantity', 'Quantity must not be empty');
        } finally {
          await scenario.close();
        }
      });
    });
  });

  // API-only test: null quantity
  it('shouldRejectOrderWithNullQuantity_API', async () => {
    const scenario = createScenario({ channel: 'api', externalSystemMode: 'stub' });
    try {
      await scenario
        .when()
        .placeOrder()
        .withQuantity(null)
        .then()
        .shouldFail()
        .errorMessage('The request contains one or more validation errors')
        .fieldErrorMessage('quantity', 'Quantity must not be empty');
    } finally {
      await scenario.close();
    }
  });
});
