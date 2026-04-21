import { test, forChannels, ChannelType } from './base/BaseAcceptanceTest.js';
import { OrderStatus } from '../../../../src/testkit/common/dtos.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
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
