import { createScenario } from '../../../../../src/test-setup';

describe('Clock Stub Contract Test', () => {
  it('shouldBeAbleToGetTime', async () => {
    const scenario = createScenario({ channel: 'api', externalSystemMode: 'stub' });
    try {
      await scenario.given().then().clock().hasTime();
    } finally {
      await scenario.close();
    }
  });
});
