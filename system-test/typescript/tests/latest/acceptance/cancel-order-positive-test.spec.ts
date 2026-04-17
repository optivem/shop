import { test, forChannels, ChannelType } from './base/fixtures.js';
import { OrderStatus } from '../../../src/testkit/common/dtos.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldHaveCancelledStatusWhenCancelled', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .when()
            .cancelOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasStatus(OrderStatus.CANCELLED);
    });
});
