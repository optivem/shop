import { test, forChannels, ChannelType } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';
import { DEFAULTS } from '../../../src/testkit/dsl/core/scenario/defaults.js';

const nonExistentOrderCases = [
    { orderNumber: 'NON-EXISTENT-ORDER-99999', message: 'Order NON-EXISTENT-ORDER-99999 does not exist.' },
    { orderNumber: 'NON-EXISTENT-ORDER-88888', message: 'Order NON-EXISTENT-ORDER-88888 does not exist.' },
    { orderNumber: 'NON-EXISTENT-ORDER-77777', message: 'Order NON-EXISTENT-ORDER-77777 does not exist.' },
];

test.eachAlsoFirstRow(nonExistentOrderCases)(
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

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('customerShouldNotBeAbleToViewAnotherCustomersOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer('B')
            .and()
            .loggedInAsCustomer('A')
            .when()
            .viewOrder()
            .then()
            .shouldFail()
            .errorMessage(`Order ${DEFAULTS.ORDER_NUMBER} does not exist.`);
    });
});

forChannels(ChannelType.API)(() => {
    test('anonymousShouldNotBeAbleToViewOrder', async ({ scenario }) => {
        await scenario
            .given()
            .notLoggedIn()
            .when()
            .viewOrder()
            .then()
            .shouldFail()
            .errorMessage(ErrorMessages.AUTHENTICATION_REQUIRED);
    });
});
