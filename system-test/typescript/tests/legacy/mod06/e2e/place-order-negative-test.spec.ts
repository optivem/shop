import { test, expect, forChannels, ChannelType } from './base/BaseE2eTest.js';
import { assertThatResult } from '../../../../src/testkit/common/result-assert.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldRejectOrderWithNonIntegerQuantity', async ({ myShopDriver }) => {
        const result = await myShopDriver.placeOrder({ sku: 'SOME-SKU', quantity: '3.5', country: 'US' });

        expect(result.success).toBe(false);
        const error = assertThatResult(result).getError();
        expect(error.message).toContain('The request contains one or more validation errors');
        const quantityError = error.fieldErrors.find((e) => e.field === 'quantity');
        expect(quantityError?.message).toBe('Quantity must be an integer');
    });
});
