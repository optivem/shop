import { uiTest as test, expect } from './base/BaseE2eTest.js';
import { assertThatResult } from '../../../../src/testkit/common/result-assert.js';
import { randomUUID } from 'node:crypto';
import { NewOrderPage } from '../../../../src/testkit/driver/adapter/ui/client/pages/NewOrderPage.js';

test('shouldPlaceOrderForValidInput', async ({ myShopUiClient, erpClient }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;

    // Given: create product in real ERP
    const productResult = await erpClient.createProduct({ sku, price: '20.00' });
    expect(productResult.success).toBe(true);

    // When: place order via UI client page objects
    const homeResult = await myShopUiClient.openHomePage();
    expect(homeResult.success).toBe(true);
    await assertThatResult(homeResult).getValue().clickNewOrder();

    const newOrderPage = myShopUiClient.newOrderPage();
    await newOrderPage.inputSku(sku);
    await newOrderPage.inputQuantity('5');
    await newOrderPage.inputCountry('US');
    await newOrderPage.clickPlaceOrder();

    // Then: success notification with order number
    const notificationResult = await newOrderPage.getResult();
    expect(notificationResult.success).toBe(true);
    const orderNumber = NewOrderPage.getOrderNumber(assertThatResult(notificationResult).getValue());
    expect(orderNumber).not.toBeNull();
});
