import { test, forChannels } from './base/fixtures.js';
import { OrderStatus } from '../../../src/common/dtos.js';

forChannels('ui', 'api')(() => {
    test('shouldHaveCancelledStatusWhenCancelled', async ({ scenario }) => {
        await scenario
            .given()
            .order()
            .when()
            .cancelOrder()
            .then()
            .shouldSucceed()
            .and()
            .order()
            .hasStatus(OrderStatus.CANCELLED);
    });
});
