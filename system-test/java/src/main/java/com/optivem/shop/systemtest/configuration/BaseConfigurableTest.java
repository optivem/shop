package com.optivem.shop.systemtest.configuration;

import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.driver.port.external.clock.ClockDriver;
import com.optivem.shop.testkit.driver.port.external.erp.ErpDriver;
import com.optivem.shop.testkit.driver.port.external.tax.TaxDriver;
import com.optivem.shop.testkit.channel.ChannelType;
import com.optivem.shop.testkit.dsl.port.ChannelMode;
import com.optivem.shop.testkit.driver.port.shop.ShopDriver;
import com.optivem.shop.testkit.driver.adapter.external.clock.ClockRealDriver;
import com.optivem.shop.testkit.driver.adapter.external.clock.ClockStubDriver;
import com.optivem.shop.testkit.driver.adapter.external.erp.ErpRealDriver;
import com.optivem.shop.testkit.driver.adapter.external.erp.ErpStubDriver;
import com.optivem.shop.testkit.driver.adapter.external.tax.TaxRealDriver;
import com.optivem.shop.testkit.driver.adapter.external.tax.TaxStubDriver;
import com.optivem.shop.testkit.driver.adapter.shop.api.ShopApiDriver;
import com.optivem.shop.testkit.driver.adapter.shop.ui.ShopUiDriver;
import com.optivem.shop.systemtest.infrastructure.playwright.BrowserLifecycleExtension;
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
        var externalSystemMode = com.optivem.shop.testkit.dsl.port.ExternalSystemMode.valueOf(
                configuration.getExternalSystemMode().name());

        return new UseCaseDsl(
                externalSystemMode,
                configuration.getChannelMode(),
                channel -> createShopDriverForChannel(configuration, channel),
                () -> createErpDriver(configuration),
                () -> createClockDriver(configuration),
                () -> createTaxDriver(configuration)
        );
    }

    private ShopDriver createShopDriverForChannel(Configuration configuration, String channel) {
        if (ChannelType.UI.equals(channel)) {
            return new ShopUiDriver(configuration.getShopUiBaseUrl(), BrowserLifecycleExtension.getBrowser());
        } else if (ChannelType.API.equals(channel)) {
            return new ShopApiDriver(configuration.getShopApiBaseUrl());
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
