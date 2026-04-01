import { createScenario } from '../../../../src/test-setup';

describe('Clock Stub Contract Test', () => {
  it('shouldBeAbleToGetTime', async () => {
    const scenario = createScenario({ channel: 'api', externalSystemMode: 'stub' });
    try {
      await scenario.given().clock().withTime().then().clock().hasTime();
    } finally {
      await scenario.close();
    }
  });

  it('shouldBeAbleToGetConfiguredTime', async () => {
    const scenario = createScenario({ channel: 'api', externalSystemMode: 'stub' });
    try {
      await scenario
        .given()
        .clock()
        .withTime('2024-01-02T09:00:00Z')
        .then()
        .clock()
        .hasTime('2024-01-02T09:00:00Z');
    } finally {
      await scenario.close();
    }
  });
});
