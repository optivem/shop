import { loadConfiguration } from '../../../../../config/configuration-loader';

describe('ERP Smoke Test', () => {
  it('shouldBeAbleToGoToErp', async () => {
    const config = loadConfiguration();

    const response = await fetch(`${config.externalSystems.erp.url}/health`);

    expect(response.status).toBe(200);
  });
});
