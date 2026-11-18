import { test, forChannels, ChannelType } from './base/fixtures.js';
import { criteria } from '../../commons/criteria.js';

// API only: the UI offers the customers as the order history's customer filter, which
// browse-order-history-positive-test's shouldOfferTheCustomersWhoHavePlacedOrders covers.
forChannels(ChannelType.API)(() => {
    // Shared stack: every customer who has ever ordered is listed, so only A and B's relative order is asserted.
    test('shouldListEachCustomerWhoHasPlacedOrdersOnceFromAToZ', criteria('UC-011.3'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .withOrderNumber('ORD-A1')
            .placedByCustomer('A')
            .and()
            .order()
            .withOrderNumber('ORD-B')
            .placedByCustomer('B')
            .and()
            .order()
            .withOrderNumber('ORD-A2')
            .placedByCustomer('A')
            .and()
            .loggedInAsAdmin()
            .when()
            .browseCustomers()
            .then()
            .shouldSucceed()
            .customerList()
            .listsCustomersInOrder('A', 'B');
    });
});
