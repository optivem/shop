package com.mycompany.myshop.backend.component.latest.smoke.system;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;

/**
 * "After" of the component-test refactor: the harness canary written on the scenario DSL
 * ({@link com.mycompany.myshop.backend.testkit.dsl.core.ScenarioDslImpl}) instead of raw {@code
 * restTemplate}. Same assertion as the {@code legacy/} twin — proves the in-process harness boots
 * and serves (random port/real socket, Testcontainers Postgres migrated and reachable, HTTP
 * round-trips work), with no compose.
 *
 * <p>{@code assume().myShop().shouldBeRunning()} resolves to the {@code GET /health} liveness probe,
 * so the canary depends only on the harness being up, not on any feature endpoint. The {@code
 * legacy/} restTemplate twin remains the dependency-light canary.
 */
class MyShopSmokeTest extends BaseComponentTest {

    @Test
    void shouldBeAbleToGoToMyShop() {
        scenario.assume().myShop().shouldBeRunning();
    }
}
