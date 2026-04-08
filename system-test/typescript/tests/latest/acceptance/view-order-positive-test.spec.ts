import { test, forChannels } from './base/fixtures.js';

forChannels('ui', 'api')(() => {
    test('shouldBeAbleToViewOrder', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .when()
            .viewOrder()
            .then()
            .shouldSucceed();
    });
});
