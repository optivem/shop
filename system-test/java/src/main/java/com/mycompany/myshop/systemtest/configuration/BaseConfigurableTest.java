package com.mycompany.myshop.systemtest.configuration;

import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.driver.port.external.tax.TaxDriver;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.adapter.external.clock.ClockRealDriver;
import com.mycompany.myshop.testkit.driver.adapter.external.clock.ClockStubDriver;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.ErpRealDriver;
import com.mycompany.myshop.testkit.driver.adapter.external.erp.ErpStubDriver;
import com.mycompany.myshop.testkit.driver.adapter.external.tax.TaxRealDriver;
import com.mycompany.myshop.testkit.driver.adapter.external.tax.TaxStubDriver;
import com.mycompany.myshop.testkit.driver.adapter.api.MyShopApiDriver;
import com.mycompany.myshop.testkit.driver.adapter.ui.MyShopUiDriver;
import com.mycompany.myshop.systemtest.infrastructure.playwright.BrowserLifecycleExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BrowserLifecycleExtension.class)
public abstract class BaseConfigurableTest {
    protected Environment getFixedEnvironment() {
        return null;
    }

    protected ExternalSystemMode getFixedExternalSystemMode() {
        return null;
    }

    protected ChannelMode getFixedChannelMode() {
        return null;
    }

    protected Configuration loadConfiguration() {
        var environment = PropertyLoader.getEnvironment(getFixedEnvironment());
        var externalSystemMode = PropertyLoader.getExternalSystemMode(getFixedExternalSystemMode());
        var channelMode = PropertyLoader.getChannelMode(getFixedChannelMode());

        return ConfigurationLoader.load(environment, externalSystemMode, channelMode);
    }

    protected UseCaseDsl createUseCaseDsl(Configuration configuration) {
        return new UseCaseDsl(
                configuration.getExternalSystemMode(),
                configuration.getChannelMode(),
                channel -> createMyShopDriverForChannel(configuration, channel),
                () -> createErpDriver(configuration),
                () -> createClockDriver(configuration),
                () -> createTaxDriver(configuration)
        );
    }

    private MyShopDriver createMyShopDriverForChannel(Configuration configuration, String channel) {
        if (ChannelType.UI.equals(channel)) {
            return new MyShopUiDriver(configuration.getMyShopUiBaseUrl(), BrowserLifecycleExtension.getBrowser());
        } else if (ChannelType.API.equals(channel)) {
            return new MyShopApiDriver(configuration.getMyShopApiBaseUrl());
        } else {
            throw new IllegalStateException("Unknown channel: " + channel);
        }
    }

    private ErpDriver createErpDriver(Configuration configuration) {
        return switch (configuration.getExternalSystemMode()) {
            case REAL -> new ErpRealDriver(configuration.getErpBaseUrl());
            case STUB -> new ErpStubDriver(configuration.getErpBaseUrl());
        };
    }

    private ClockDriver createClockDriver(Configuration configuration) {
        return switch (configuration.getExternalSystemMode()) {
            case REAL -> new ClockRealDriver();
            case STUB -> new ClockStubDriver(configuration.getClockBaseUrl());
        };
    }

    private TaxDriver createTaxDriver(Configuration configuration) {
        return switch (configuration.getExternalSystemMode()) {
            case REAL -> new TaxRealDriver(configuration.getTaxBaseUrl());
            case STUB -> new TaxStubDriver(configuration.getTaxBaseUrl());
        };
    }
}
