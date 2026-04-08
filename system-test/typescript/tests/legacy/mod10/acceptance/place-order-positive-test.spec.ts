import { test, forChannels } from './fixtures.js';
import { OrderStatus } from '../../../../src/common/dtos.js';

forChannels('ui', 'api')(() => {
    test('orderNumberShouldStartWithORD', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasOrderNumberPrefix('ORD-');
    });

    test('orderStatusShouldBePlacedAfterPlacingOrder', async ({ scenario }) => {
        await scenario
            .when()
            .placeOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasStatus(OrderStatus.PLACED);
    });
});
