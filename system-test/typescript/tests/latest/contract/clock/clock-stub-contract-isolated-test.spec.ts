process.env.EXTERNAL_SYSTEM_MODE = 'stub';

import { test } from '../base/fixtures.js';

test.describe('@isolated', () => {
    test.describe.configure({ mode: 'serial' });
    test('shouldBeAbleToGetConfiguredTime', async ({ scenario }) => {
        await scenario
            .given()
            .clock()
            .withTime('2024-01-02T09:00:00Z')
            .then()
            .clock()
            .hasTime('2024-01-02T09:00:00Z');
    });
});
