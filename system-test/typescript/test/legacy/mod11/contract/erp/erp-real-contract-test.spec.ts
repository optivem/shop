import { createScenario } from '../../../../../src/test-setup';

describe('ERP Real Contract Test', () => {
  it('shouldBeAbleToGetProduct', async () => {
    const scenario = createScenario({ channel: 'api', externalSystemMode: 'real' });
    try {
      await scenario
        .given()
        .product()
        .withSku('SKU-123')
        .withUnitPrice(12.0)
        .then()
        .product('SKU-123')
        .hasSku('SKU-123')
        .hasPrice(12.0);
    } finally {
      await scenario.close();
    }
  });
});
