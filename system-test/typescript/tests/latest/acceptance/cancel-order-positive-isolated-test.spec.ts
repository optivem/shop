import { test } from './base/fixtures.js';
import { OrderStatus } from '../../../src/testkit/common/dtos.js';

const timesOutsideBlackout = [
    '2024-12-31T21:59:59Z',
    '2024-12-31T22:30:01Z',
    '2024-12-31T10:00:00Z',
    '2025-01-01T22:15:00Z',
];

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });
    test.eachAlsoFirstRow(timesOutsideBlackout)(
        'shouldBeAbleToCancelOrderOutsideOfBlackoutPeriod31stDecBetween2200And2230_$time @time-dependent',
        async ({ scenario, time }) => {
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
                .shouldSucceed();
        },
    );
});
