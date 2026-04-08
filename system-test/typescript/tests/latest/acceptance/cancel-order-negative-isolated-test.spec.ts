import { test, forChannels } from './base/fixtures.js';
import { OrderStatus } from '../../../src/common/dtos.js';

const timesInsideBlackout = [
    '2024-12-31T22:00:00Z',
    '2026-12-31T22:00:01Z',
    '2025-12-31T22:15:00Z',
    '2028-12-31T22:29:59Z',
    '2021-12-31T22:30:00Z',
];

const BLACKOUT_ERROR = 'Order cancellation is not allowed on December 31st between 22:00 and 23:00';

forChannels('ui', 'api')(() => {
    timesInsideBlackout.forEach((time) => {
        test(`cannotCancelAnOrderOn31stDecBetween2200And2230_${time}`, async ({ scenario }) => {
            await scenario
                .given()
                .clock()
                .withTime(time)
                .and()
                .order()
                .withStatus(OrderStatus.PLACED)
                .when()
                .cancelOrder()
                .then()
                .shouldFail()
                .errorMessage(BLACKOUT_ERROR);
        });
    });
});
