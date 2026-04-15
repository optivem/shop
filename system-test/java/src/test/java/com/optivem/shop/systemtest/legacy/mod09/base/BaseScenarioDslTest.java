package com.optivem.shop.systemtest.legacy.mod09.base;

import com.optivem.shop.systemtest.configuration.BaseConfigurableTest;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.ScenarioDslImpl;
import com.optivem.shop.testkit.dsl.port.ScenarioDsl;
import com.optivem.shop.testkit.common.Closer;
import com.optivem.shop.systemtest.infrastructure.playwright.BrowserLifecycleExtension;
import com.optivem.testing.extensions.ChannelExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({ChannelExtension.class, BrowserLifecycleExtension.class})
public class BaseScenarioDslTest extends BaseConfigurableTest {
    private UseCaseDsl app;
    protected ScenarioDsl scenario;

    @BeforeEach
    void setUp() {
        var configuration = loadConfiguration();
        app = createUseCaseDsl(configuration);
        scenario = new ScenarioDslImpl(app);
    }

    @AfterEach
    void tearDown() {
        Closer.close(app);
    }
}

