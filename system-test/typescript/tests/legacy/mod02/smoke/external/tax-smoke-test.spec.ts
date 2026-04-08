import { test, expect } from '@playwright/test';
import { loadConfiguration } from '../../../../../config/configuration-loader.js';

test('shouldBeAbleToGoToTax', async () => {
    const config = loadConfiguration();
    const response = await fetch(`${config.externalSystems.tax.url}/health`);
    expect(response.status).toBe(200);
});
