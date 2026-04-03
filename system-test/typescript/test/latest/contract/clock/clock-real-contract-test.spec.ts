import { createScenario } from '../../../../src/test-setup';

describe('Clock Real Contract Test', () => {
  it('shouldBeAbleToGetTime', async () => {
    const scenario = createScenario({ channel: 'api', externalSystemMode: 'real' });
    try {
      await scenario.given().then().clock().hasTime();
    } finally {
      await scenario.close();
    }
  });
});
