import { test, forChannels, ChannelType } from './base/fixtures.js';
import { OrderStatus } from '../../../src/testkit/common/dtos.js';
import { criteria } from '../../commons/criteria.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldHaveCancelledStatusWhenCancelled', criteria('UC-003.1'), async ({ scenario }) => {
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

    test('adminShouldBeAbleToCancelCustomerOrder', criteria('UC-003.4'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsAdmin()
            .when()
            .cancelOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasStatus(OrderStatus.CANCELLED);
    });
});
