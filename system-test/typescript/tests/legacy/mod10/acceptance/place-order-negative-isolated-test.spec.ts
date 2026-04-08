import { test, forChannels } from './fixtures.js';

forChannels('ui', 'api')(() => {
    test('shouldRejectOrderPlacedAtYearEnd', async ({ scenario }) => {
        await scenario
            .given()
            .clock()
            .withTime('2026-12-31T23:59:30Z')
            .when()
            .placeOrder()
            .then()
            .shouldFail()
            .errorMessage('Orders cannot be placed between 23:59 and 00:00 on December 31st');
    });
});
