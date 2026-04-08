import { test, forChannels } from './base/fixtures.js';
import { OrderStatus } from '../../../src/common/dtos.js';

const nonExistentOrderCases = [
    { orderNumber: 'NON-EXISTENT-ORDER-99999', message: 'Order NON-EXISTENT-ORDER-99999 does not exist.' },
    { orderNumber: 'NON-EXISTENT-ORDER-88888', message: 'Order NON-EXISTENT-ORDER-88888 does not exist.' },
    { orderNumber: 'NON-EXISTENT-ORDER-77777', message: 'Order NON-EXISTENT-ORDER-77777 does not exist.' },
];

forChannels('api')(() => {
    test.each(nonExistentOrderCases)(
        'shouldNotCancelNonExistentOrder_$orderNumber',
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

    test('shouldNotCancelAlreadyCancelledOrder', async ({ scenario }) => {
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

    test('cannotCancelNonExistentOrder', async ({ scenario }) => {
        await scenario
            .when()
            .cancelOrder()
            .withOrderNumber('non-existent-order-12345')
            .then()
            .shouldFail()
            .errorMessage('Order non-existent-order-12345 does not exist.');
    });
});
