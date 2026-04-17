import { test, forChannels, ChannelType } from './fixtures.js';
import { OrderStatus } from '../../../../src/testkit/common/dtos.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldPlaceOrderForValidInput', async ({ scenario }) => {
        await scenario
            .given()
            .product()
            .withUnitPrice(20)
            .when()
            .placeOrder()
            .withQuantity(5)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasOrderNumberPrefix('ORD-')
            .hasQuantity(5)
            .hasUnitPrice(20)
            .hasStatus(OrderStatus.PLACED)
            .hasTotalPriceGreaterThanZero();
    });
});
