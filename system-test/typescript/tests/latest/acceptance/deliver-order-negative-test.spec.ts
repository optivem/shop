import { test, forChannels, ChannelType } from './base/fixtures.js';
import { ErrorMessages } from '../../commons/constants/error-messages.js';
import { OrderStatus } from '../../../src/testkit/common/dtos.js';
import { criteria } from '../../commons/criteria.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('customerShouldNotBeAbleToDeliverOrder', criteria('UC-012.4'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsCustomer()
            .when()
            .deliverOrder()
            .then()
            .shouldFail()
            .errorMessage(ErrorMessages.PERMISSION_DENIED)
            .and()
            .order()
            .hasStatus(OrderStatus.PLACED);
    });
});

forChannels(ChannelType.API)(() => {
    test('shouldNotDeliverNonExistentOrder', criteria('UC-012.5'), async ({ scenario }) => {
        await scenario
            .given()
            .loggedInAsAdmin()
            .when()
            .deliverOrder()
            .withOrderNumber('NON-EXISTENT-ORDER-99999')
            .then()
            .shouldFail()
            .errorMessage('Order NON-EXISTENT-ORDER-99999 does not exist.');
    });

    test.each([{ status: OrderStatus.CANCELLED }, { status: OrderStatus.DELIVERED }])(
        'shouldNotDeliverOrderThatIsNotPlaced_$status',
        criteria('UC-012.6'),
        async ({ scenario, status }) => {
            await scenario
                .given()
                .order()
                .placedByCustomer()
                .withStatus(status)
                .and()
                .loggedInAsAdmin()
                .when()
                .deliverOrder()
                .then()
                .shouldFail()
                .errorMessage('Order cannot be delivered in its current status')
                .and()
                .order()
                .hasStatus(status);
        },
    );
});

forChannels(ChannelType.UI)(() => {
    test('shouldOfferNeitherDeliverNorCancelOnceTheOrderIsDelivered', criteria('UC-012.2'), async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .withStatus(OrderStatus.DELIVERED)
            .and()
            .loggedInAsAdmin()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .doesNotOfferDeliverOrder()
            .doesNotOfferCancelOrder();
    });
});
