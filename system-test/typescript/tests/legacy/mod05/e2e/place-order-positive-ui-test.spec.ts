import { uiTest as test, expect } from './fixtures.js';

test('shouldPlaceOrderForValidInput', async ({ shopDriver, erpDriver, taxDriver }) => {
    const sku = 'DEFAULT-SKU';

    // Given: set up product and tax via driver ports
    const productResult = await erpDriver.returnsProduct({ sku, price: '20.00' });
    expect(productResult.success).toBe(true);

    const taxResult = await taxDriver.returnsTaxRate({ country: 'US', taxRate: '0.07' });
    expect(taxResult.success).toBe(true);

    // When: place order via shop driver (UI)
    const result = await shopDriver.placeOrder({ sku, quantity: '5', country: 'US' });

    // Then
    expect(result.success).toBe(true);
    if (result.success) {
        expect(result.value.orderNumber).toMatch(/^ORD-/);

        const viewResult = await shopDriver.viewOrder(result.value.orderNumber);
        expect(viewResult.success).toBe(true);
        if (viewResult.success) {
            expect(viewResult.value.quantity).toBe(5);
            expect(viewResult.value.unitPrice).toBe(20);
            expect(viewResult.value.status).toBe('PLACED');
            expect(viewResult.value.totalPrice).toBeGreaterThan(0);
        }
    }
});
