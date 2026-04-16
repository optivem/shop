process.env.EXTERNAL_SYSTEM_MODE = 'real';

import { test } from '../fixtures.js';

test('shouldBeAbleToGetTime', async ({ scenario }) => {
    await scenario.given().then().clock().hasTime();
});
