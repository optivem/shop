import { test, forChannels } from './fixtures.js';
import { OrderStatus } from '../../../../src/common/dtos.js';

forChannels('ui', 'api')(() => {
    test('shouldPlaceOrderForValidInput', async ({ scenario }) => {
        await scenario
            .given()
            .product()
            .withUnitPrice(20.0)
            .when()
            .placeOrder()
            .withQuantity(5)
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasOrderNumberPrefix('ORD-')
            .hasStatus(OrderStatus.PLACED);
    });
});
