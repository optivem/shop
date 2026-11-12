import { test, forChannels, ChannelType } from './base/fixtures.js';
import type { ScenarioDsl } from '../../../src/testkit/dsl/scenario-dsl.js';
import { OrderStatus } from '../../../src/testkit/common/dtos.js';
import { criteria } from '../../commons/criteria.js';

// My orders for a customer of their own, whose orders are placed at set times. The orders are placed out of date order
// (ORD-1002, ORD-1001, ORD-1003), so a list in placing order would not pass for one in date order.
function givenANewCustomersThreeOrders(scenario: ScenarioDsl) {
    return scenario
        .given()
        .newCustomer()
        .and()
        .order()
        .withOrderNumber('ORD-1002')
        .placedAt('2026-01-12T14:30:00Z')
        .and()
        .order()
        .withOrderNumber('ORD-1001')
        .placedAt('2026-01-10T09:00:00Z')
        .and()
        .order()
        .withOrderNumber('ORD-1003')
        .placedAt('2026-01-15T08:00:00Z')
        .and()
        .loggedInAsCustomer();
}

const pageCases = [
    { page: '1', orders: ['ORD-1003', 'ORD-1002'] },
    { page: '2', orders: ['ORD-1001'] },
    { page: '3', orders: [] },
];

// Sets the shared clock to place each order at its own time; parallel runs would clash on the clock.
test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });

    forChannels(ChannelType.UI, ChannelType.API)(() => {
        test('shouldListTheCustomersOrdersNewestFirst', criteria('UC-002.1'), async ({ scenario }) => {
            await givenANewCustomersThreeOrders(scenario)
                .when()
                .browseOrderHistory()
                .withNoOrderNumberFilter()
                .then()
                .shouldSucceed()
                .orderHistory()
                .showsOrders('ORD-1003', 'ORD-1002', 'ORD-1001');
        });

        test('shouldShowTheSummaryOfEachOrder', criteria('UC-002.1'), async ({ scenario }) => {
            await scenario
                .given()
                .product()
                .withSku('BOOK-123')
                .withUnitPrice(20.0)
                .and()
                .country()
                .withCode('US')
                .withTaxRate('0.10')
                .and()
                .coupon()
                .withCouponCode('SUMMER2025')
                .withDiscountRate(0.1)
                .and()
                .order()
                .withOrderNumber('ORD-1001')
                .withSku('BOOK-123')
                .withQuantity(5)
                .withCountry('US')
                .withCouponCode('SUMMER2025')
                .placedAt('2026-01-10T09:00:00Z')
                .and()
                .loggedInAsCustomer()
                .when()
                .browseOrderHistory()
                .withOrderNumber('ORD-1001')
                .then()
                .shouldSucceed()
                .orderHistory()
                .showsOrderSummary('ORD-1001', '2026-01-10T09:00:00Z', 'BOOK-123', 5, '99.00', OrderStatus.PLACED);
        });

        test('shouldFilterOrdersByPartOfTheOrderNumberIgnoringCase', criteria('UC-002.2'), async ({ scenario }) => {
            await givenANewCustomersThreeOrders(scenario)
                .when()
                .browseOrderHistory()
                .withOrderNumberPartOf('ORD-1002')
                .then()
                .shouldSucceed()
                .orderHistory()
                .showsOrders('ORD-1002');
        });

        // The spec's "ord-100" is the part every order number here shares: the generated numbers share only "ORD-".
        test('shouldShowEveryOrderWhoseNumberHasTheFilterIgnoringCase', criteria('UC-002.2'), async ({ scenario }) => {
            await givenANewCustomersThreeOrders(scenario)
                .when()
                .browseOrderHistory()
                .withOrderNumber('ord-')
                .then()
                .shouldSucceed()
                .orderHistory()
                .showsOrders('ORD-1003', 'ORD-1002', 'ORD-1001');
        });

        test('shouldShowEveryOrderWhenTheFilterIsEmpty', criteria('UC-002.2'), async ({ scenario }) => {
            await givenANewCustomersThreeOrders(scenario)
                .when()
                .browseOrderHistory()
                .withOrderNumber('')
                .then()
                .shouldSucceed()
                .orderHistory()
                .showsOrders('ORD-1003', 'ORD-1002', 'ORD-1001');
        });
    });

    forChannels(ChannelType.API)(() => {
        test.each(pageCases)('shouldReadTheOrdersOnePageAtATime_$page', criteria('UC-002.3'), async ({ scenario, page, orders }) => {
            await givenANewCustomersThreeOrders(scenario)
                .when()
                .browseOrderHistory()
                .withNoOrderNumberFilter()
                .withPage(page)
                .withSize('2')
                .then()
                .shouldSucceed()
                .orderHistory()
                .showsOrders(...orders)
                .hasTotalElements(3);
        });
    });
});
