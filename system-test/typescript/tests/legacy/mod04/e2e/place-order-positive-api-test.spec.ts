import { apiTest as test, expect } from './base/BaseE2eTest.js';
import { randomUUID } from 'node:crypto';
import { assertThatResult } from '../../../../src/testkit/common/result-assert.js';

test('shouldPlaceOrderForValidInput', async ({ myShopApiClient, erpClient }) => {
    const sku = `SKU-${randomUUID().substring(0, 8)}`;

    // Given: create product in real ERP
    const productResult = await erpClient.createProduct({ sku, price: '20.00' });
    expect(productResult.success).toBe(true);

    // When: place order via API client
    const result = await myShopApiClient.orders().placeOrder({ sku, quantity: '5', country: 'US' });

    // Then: place order should succeed
    expect(result.success).toBe(true);
    const placedOrder = assertThatResult(result).getValue();
    expect(placedOrder.orderNumber).toMatch(/^ORD-/);

    // Then: view order returns full order details
    const viewResult = await myShopApiClient.orders().viewOrder(placedOrder.orderNumber);
    expect(viewResult.success).toBe(true);
    const order = assertThatResult(viewResult).getValue();
    expect(order.orderNumber).toBe(placedOrder.orderNumber);
    expect(order.sku).toBe(sku);
    expect(order.quantity).toBe(5);
    expect(order.unitPrice).toBe(20);
    expect(order.totalPrice).toBeGreaterThan(0);
    expect(order.status).toBe('PLACED');
});
