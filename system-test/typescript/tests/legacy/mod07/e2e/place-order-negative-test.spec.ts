import { test, expect, forChannels, ChannelType } from './fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldRejectOrderWithNonIntegerQuantity', async ({ useCase }) => {
        const result = await useCase.shop().placeOrder({ sku: 'SOME-SKU', quantity: '3.5', country: 'US' });

        expect(result.success).toBe(false);
        if (!result.success) {
            expect(result.error.message).toContain('The request contains one or more validation errors');
            const quantityError = result.error.fieldErrors.find((e) => e.field === 'quantity');
            expect(quantityError?.message).toBe('Quantity must be an integer');
        }
    });
});
