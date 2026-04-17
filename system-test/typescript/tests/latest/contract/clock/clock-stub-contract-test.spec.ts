process.env.EXTERNAL_SYSTEM_MODE = 'stub';

import { test } from '../base/fixtures.js';

test('shouldBeAbleToGetTime', async ({ scenario }) => {
    await scenario.given().clock().withTime().then().clock().hasTime();
});
