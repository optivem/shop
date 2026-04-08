import { test, forChannels } from './base/fixtures.js';

const nonExistentOrderCases = [
    { orderNumber: 'NON-EXISTENT-ORDER-99999', message: 'Order NON-EXISTENT-ORDER-99999 does not exist.' },
    { orderNumber: 'NON-EXISTENT-ORDER-88888', message: 'Order NON-EXISTENT-ORDER-88888 does not exist.' },
    { orderNumber: 'NON-EXISTENT-ORDER-77777', message: 'Order NON-EXISTENT-ORDER-77777 does not exist.' },
];

forChannels('api')(() => {
    test.each(nonExistentOrderCases)(
        'shouldNotBeAbleToViewNonExistentOrder_$orderNumber',
        async ({ scenario, orderNumber, message }) => {
            await scenario
                .when()
                .viewOrder()
                .withOrderNumber(orderNumber)
                .then()
                .shouldFail()
                .errorMessage(message);
        },
    );
});
