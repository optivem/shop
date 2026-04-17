import { test, expect } from '@playwright/test';
import { getTaxBaseUrl } from '../../base/BaseRawTest.js';

test('shouldBeAbleToGoToTax', async () => {
    const response = await fetch(`${getTaxBaseUrl()}/health`);
    expect(response.status).toBe(200);
});
