import { test, expect } from '@playwright/test';
import { getShopApiBaseUrl } from '../../base/BaseRawTest.js';

test('shouldBeAbleToGoToShop', async () => {
    const response = await fetch(`${getShopApiBaseUrl()}/health`);
    expect(response.status).toBe(200);
});
