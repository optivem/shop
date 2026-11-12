import { test, forChannels, ChannelType } from './base/fixtures.js';
import { OrderStatus } from '../../../src/testkit/common/dtos.js';
import { DEFAULTS } from '../../../src/testkit/dsl/core/scenario/defaults.js';
import { criteria } from '../../commons/criteria.js';

const nonExistentOrderCases = [
    { orderNumber: 'NON-EXISTENT-ORDER-99999', message: 'Order NON-EXISTENT-ORDER-99999 does not exist.' },
    { orderNumber: 'NON-EXISTENT-ORDER-88888', message: 'Order NON-EXISTENT-ORDER-88888 does not exist.' },
    { orderNumber: 'NON-EXISTENT-ORDER-77777', message: 'Order NON-EXISTENT-ORDER-77777 does not exist.' },
];

forChannels(ChannelType.API)(() => {
    test.each(nonExistentOrderCases)(
        'shouldNotCancelNonExistentOrder_$orderNumber',
        criteria('UC-003.6'),
        async ({ scenario, orderNumber, message }) => {
            await scenario
                .when()
                .cancelOrder()
                .withOrderNumber(orderNumber)
                .then()
                .shouldFail()
                .errorMessage(message);
        },
    );

    test('shouldNotCancelAlreadyCancelledOrder', criteria('UC-003.7'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .withStatus(OrderStatus.CANCELLED)
            .when()
            .cancelOrder()
            .then()
            .shouldFail()
            .errorMessage('Order has already been cancelled');
    });

    test('cannotCancelNonExistentOrder', criteria('UC-003.6'), async ({ scenario }) => {
        await scenario
            .when()
            .cancelOrder()
            .withOrderNumber('non-existent-order-12345')
            .then()
            .shouldFail()
            .errorMessage('Order non-existent-order-12345 does not exist.');
    });

    test('customerShouldNotBeAbleToCancelAnotherCustomersOrder', criteria('UC-003.6'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer('B')
            .and()
            .loggedInAsCustomer('A')
            .when()
            .cancelOrder()
            .then()
            .shouldFail()
            .errorMessage(`Order ${DEFAULTS.ORDER_NUMBER} does not exist.`);
    });
});

forChannels(ChannelType.UI)(() => {
    test.each([{ status: OrderStatus.CANCELLED }, { status: OrderStatus.DELIVERED }])(
        'shouldNotOfferToCancelAnOrderThatIsNoLongerPlaced_$status',
        criteria('UC-003.2'),
        async ({ scenario, status }) => {
            await scenario
                .given()
                .order()
                .withStatus(status)
                .when()
                .viewOrder()
                .then()
                .shouldSucceed()
                .and()
                .order()
                .doesNotOfferCancelOrder();
        },
    );
});
