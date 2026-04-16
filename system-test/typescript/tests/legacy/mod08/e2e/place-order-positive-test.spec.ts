import { test, forChannels } from './fixtures.js';
import { OrderStatus } from '../../../../src/testkit/common/dtos.js';

forChannels('ui', 'api')(() => {
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
