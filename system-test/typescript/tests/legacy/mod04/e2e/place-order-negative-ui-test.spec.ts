import { uiTest as test, expect } from './base/BaseE2eTest.js';
import { assertThatResult } from '../../../../src/testkit/common/result-assert.js';

test('shouldRejectOrderWithNonIntegerQuantity', async ({ myShopUiClient }) => {
    // When: place order with invalid quantity via UI client
    const homeResult = await myShopUiClient.openHomePage();
    expect(homeResult.success).toBe(true);
    await assertThatResult(homeResult).getValue().clickNewOrder();

    const newOrderPage = myShopUiClient.newOrderPage();
    await newOrderPage.inputSku('SOME-SKU');
    await newOrderPage.inputQuantity('invalid-quantity');
    await newOrderPage.inputCountry('US');
    await newOrderPage.clickPlaceOrder();

    // Then: error result contains validation message, quantity field, integer constraint
    const result = await newOrderPage.getResult();
    expect(result.success).toBe(false);
    const error = assertThatResult(result).getError();
    expect(error.message).toContain('The request contains one or more validation errors');
    const quantityError = error.fieldErrors.find((e) => e.field === 'quantity');
    expect(quantityError?.message).toContain('Quantity must be an integer');
});
