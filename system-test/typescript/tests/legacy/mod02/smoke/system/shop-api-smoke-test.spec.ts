import { test, expect } from '@playwright/test';
import { loadConfiguration } from '../../../../../config/configuration-loader.js';

test('shouldBeAbleToGoToShop', async () => {
    const config = loadConfiguration();
    const response = await fetch(`${config.shop.backendApiUrl}/health`);
    expect(response.status).toBe(200);
});
