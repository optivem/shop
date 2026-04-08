import { test, expect } from '@playwright/test';
import { loadConfiguration } from '../../../../../config/configuration-loader.js';

test('shouldBeAbleToGoToErp', async () => {
    const config = loadConfiguration();
    const response = await fetch(`${config.externalSystems.erp.url}/health`);
    expect(response.status).toBe(200);
});
