import { test } from '../fixtures.js';

test('shouldBeAbleToGoToIdentityProvider', async ({ scenario }) => {
    await scenario.assume().identityProvider().shouldBeRunning();
});
