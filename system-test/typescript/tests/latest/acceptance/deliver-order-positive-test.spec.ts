import { test, forChannels, ChannelType } from './base/fixtures.js';
import { OrderStatus } from '../../../src/testkit/common/dtos.js';
import { criteria } from '../../commons/criteria.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldHaveDeliveredStatusWhenDelivered', criteria('UC-012.1'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsAdmin()
            .when()
            .deliverOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasStatus(OrderStatus.DELIVERED);
    });

    test('adminShouldBeAbleToDeliverAnotherCustomersOrder', criteria('UC-012.3'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer('B')
            .and()
            .loggedInAsAdmin()
            .when()
            .deliverOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasStatus(OrderStatus.DELIVERED);
    });
});

forChannels(ChannelType.UI)(() => {
    test('shouldOfferToDeliverAPlacedOrder', criteria('UC-012.2'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsAdmin()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .offersDeliverOrder();
    });
});
