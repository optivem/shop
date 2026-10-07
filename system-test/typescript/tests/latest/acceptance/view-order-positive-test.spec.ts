import { test, forChannels, ChannelType } from './base/fixtures.js';

forChannels(ChannelType.UI, ChannelType.API)(() => {
    test('shouldBeAbleToViewOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });

    test('customerShouldBeAbleToViewOwnOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsCustomer()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });

    test('sameCustomerAliasShouldResolveToSameCustomer', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer('B')
            .and()
            .loggedInAsCustomer('B')
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });

    test('adminShouldBeAbleToViewCustomerOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer()
            .and()
            .loggedInAsAdmin()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });

    test('adminShouldBeAbleToViewAnotherCustomersOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .placedByCustomer('B')
            .and()
            .loggedInAsAdmin()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });
});
