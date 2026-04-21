import { uiTest as test, expect } from './base/BaseE2eTest.js';
import { randomUUID } from 'node:crypto';
import { NewOrderPage } from '../../../../src/testkit/driver/adapter/shop/ui/client/pages/NewOrderPage.js';

test('shouldPlaceOrderForValidInput', async ({ shopUiClient, erpClient }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;

    // Given: create product in real ERP
    const productResult = await erpClient.createProduct({ sku, price: '20.00' });
    expect(productResult.success).toBe(true);

    // When: place order via UI client page objects
    const homeResult = await shopUiClient.openHomePage();
    expect(homeResult.success).toBe(true);
    if (!homeResult.success) return;
    await homeResult.value.clickNewOrder();

    const newOrderPage = shopUiClient.newOrderPage();
    await newOrderPage.inputSku(sku);
    await newOrderPage.inputQuantity('5');
    await newOrderPage.inputCountry('US');
    await newOrderPage.clickPlaceOrder();

    // Then: success notification with order number
    const notificationResult = await newOrderPage.getResult();
    expect(notificationResult.success).toBe(true);
    if (!notificationResult.success) return;
    const orderNumber = NewOrderPage.getOrderNumber(notificationResult.value);
    expect(orderNumber).not.toBeNull();
});
