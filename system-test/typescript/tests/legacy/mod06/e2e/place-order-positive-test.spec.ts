import { test, expect, forChannels, ChannelType } from './fixtures.js';
import { randomUUID } from 'node:crypto';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldPlaceOrderForValidInput', async ({ shopDriver, erpDriver, taxDriver }) => {
        const sku = `SKU-${randomUUID().substring(0, 8)}`;

        // Given
        const productResult = await erpDriver.returnsProduct({ sku, price: '20.00' });
        expect(productResult.success).toBe(true);

        const taxResult = await taxDriver.returnsTaxRate({ country: 'US', taxRate: '0.07' });
        expect(taxResult.success).toBe(true);

        // When
        const result = await shopDriver.placeOrder({ sku, quantity: '5', country: 'US' });

        // Then
        expect(result.success).toBe(true);
        if (result.success) {
            expect(result.value.orderNumber).toMatch(/^ORD-/);

            const viewResult = await shopDriver.viewOrder(result.value.orderNumber);
            expect(viewResult.success).toBe(true);
            if (viewResult.success) {
                expect(viewResult.value.status).toBe('PLACED');
            }
        }
    });
});
