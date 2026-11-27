package com.mycompany.myshop.systemtest.latest.smoke.system;

import com.mycompany.myshop.systemtest.latest.base.BaseScenarioDslTest;
import org.junit.jupiter.api.Test;

class IdentityProviderSmokeTest extends BaseScenarioDslTest {
    @Test
    void shouldBeAbleToGoToIdentityProvider() {
        scenario.assume().identityProvider().shouldBeRunning();
    }
}
