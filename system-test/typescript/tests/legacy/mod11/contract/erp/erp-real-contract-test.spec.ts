process.env.EXTERNAL_SYSTEM_MODE = 'real';

import { test } from '../fixtures.js';

test('shouldBeAbleToGetProduct', async ({ scenario }) => {
    await scenario
        .given()
        .product()
        .withSku('SKU-123')
        .withUnitPrice(12.0)
        .then()
        .product('SKU-123')
        .hasSku('SKU-123')
        .hasPrice(12.0);
});
