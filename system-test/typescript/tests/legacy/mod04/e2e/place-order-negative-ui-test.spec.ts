import { uiTest as test, expect } from './base/BaseE2eTest.js';

test('shouldRejectOrderWithNonIntegerQuantity', async ({ shopUiClient }) => {
    // When: place order with invalid quantity via UI client
    const homeResult = await shopUiClient.openHomePage();
    expect(homeResult.success).toBe(true);
    if (!homeResult.success) return;
    await homeResult.value.clickNewOrder();

    const newOrderPage = shopUiClient.newOrderPage();
    await newOrderPage.inputSku('SOME-SKU');
    await newOrderPage.inputQuantity('invalid-quantity');
    await newOrderPage.inputCountry('US');
    await newOrderPage.clickPlaceOrder();

    // Then: error result contains validation message, quantity field, integer constraint
    const result = await newOrderPage.getResult();
    expect(result.success).toBe(false);
    if (result.success) return;
    expect(result.error.message).toContain('The request contains one or more validation errors');
    const quantityError = result.error.fieldErrors.find((e) => e.field === 'quantity');
    expect(quantityError?.message).toContain('Quantity must be an integer');
});
