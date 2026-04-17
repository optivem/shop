import { test, expect } from '@playwright/test';
import { getErpBaseUrl } from '../../base/BaseRawTest.js';

test('shouldBeAbleToGoToErp', async () => {
    const response = await fetch(`${getErpBaseUrl()}/health`);
    expect(response.status).toBe(200);
});
