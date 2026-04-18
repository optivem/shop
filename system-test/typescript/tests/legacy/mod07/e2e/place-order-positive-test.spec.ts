import { test, forChannels, ChannelType } from './fixtures.js';

// Aliases resolved through UseCaseContext (matching Java/.NET/eshop-tests):
//   SKU          → context.getParamValue('sku')            → generates 'sku-<uuid>' on first use
//   ORDER_NUMBER → result alias; stored after placeOrder, retrieved by viewOrder
//   COUNTRY      → context.getParamValueOrLiteral('US')    → literal in real mode
const SKU = 'sku';
const ORDER_NUMBER = 'order-number';
const COUNTRY = 'US';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldPlaceOrderForValidInput', async ({ app }) => {
        (await app.erp().returnsProduct()
            .sku(SKU).unitPrice('20.00').execute())
            .shouldSucceed();

        (await app.shop().placeOrder()
            .orderNumber(ORDER_NUMBER)
            .sku(SKU).quantity(5).country(COUNTRY).execute())
            .shouldSucceed()
            .orderNumber(ORDER_NUMBER)
            .orderNumberStartsWith('ORD-');

        (await app.shop().viewOrder()
            .orderNumber(ORDER_NUMBER).execute())
            .shouldSucceed()
            .orderNumber(ORDER_NUMBER)
            .sku(SKU)
            .quantity(5)
            .unitPrice(20)
            .status('PLACED')
            .totalPriceGreaterThanZero();
    });
});
