import { loadConfiguration } from '../../../../../config/configuration-loader';

describe('Shop API Smoke Test', () => {
  it('shouldBeAbleToGoToShop', async () => {
    const config = loadConfiguration();

    const response = await fetch(`${config.shop.backendApiUrl}/health`);

    expect(response.status).toBe(200);
  });
});
