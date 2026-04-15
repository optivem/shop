package com.optivem.shop.systemtest.legacy.mod07.base;

import com.optivem.shop.systemtest.configuration.BaseConfigurableTest;
import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.common.Closer;
import com.optivem.testing.extensions.ChannelExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ChannelExtension.class)
public class BaseUseCaseDslTest extends BaseConfigurableTest {
    protected UseCaseDsl app;

    @BeforeEach
    void setUp() {
        var configuration = loadConfiguration();
        app = createUseCaseDsl(configuration);
    }

    @AfterEach
    void tearDown() {
        Closer.close(app);
    }
}

