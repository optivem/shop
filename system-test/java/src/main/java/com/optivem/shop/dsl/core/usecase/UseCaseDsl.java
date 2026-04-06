package com.optivem.shop.dsl.core.usecase;

import com.optivem.shop.dsl.common.Closer;
import com.optivem.shop.dsl.core.shared.UseCaseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.optivem.shop.dsl.core.usecase.external.clock.ClockDsl;
import com.optivem.shop.dsl.core.usecase.external.erp.ErpDsl;
import com.optivem.shop.dsl.core.usecase.external.tax.TaxDsl;
import com.optivem.shop.dsl.core.usecase.shop.ShopDsl;
import com.optivem.shop.dsl.channel.ChannelType;
import com.optivem.shop.dsl.driver.port.external.clock.ClockDriver;
import com.optivem.shop.dsl.driver.port.external.erp.ErpDriver;
import com.optivem.shop.dsl.driver.port.external.tax.TaxDriver;
import com.optivem.shop.dsl.driver.port.shop.ShopDriver;
import com.optivem.shop.dsl.port.ChannelMode;
import com.optivem.shop.dsl.port.ExternalSystemMode;
import com.optivem.testing.contexts.ChannelContext;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class UseCaseDsl implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(UseCaseDsl.class);
    private static final String STATIC_CHANNEL = ChannelType.API;

    private final UseCaseContext context;
    private final ChannelMode channelMode;
    private final Function<String, ShopDriver> shopDriverFactory;
    private final Supplier<ErpDriver> erpDriverSupplier;
    private final Supplier<ClockDriver> clockDriverSupplier;
    private final Supplier<TaxDriver> taxDriverSupplier;

    private final Map<String, ShopDsl> shops = new HashMap<>();
    private ErpDriver erpDriver;
    private ClockDriver clockDriver;
    private TaxDriver taxDriver;

    private ErpDsl erp;
    private ClockDsl clock;
    private TaxDsl tax;

    public UseCaseDsl(ExternalSystemMode externalSystemMode,
                     ChannelMode channelMode,
                     Function<String, ShopDriver> shopDriverFactory,
                     Supplier<ErpDriver> erpDriverSupplier,
                     Supplier<ClockDriver> clockDriverSupplier,
                     Supplier<TaxDriver> taxDriverSupplier) {
        this.context = new UseCaseContext(externalSystemMode);
        this.channelMode = channelMode;
        this.shopDriverFactory = shopDriverFactory;
        this.erpDriverSupplier = erpDriverSupplier;
        this.clockDriverSupplier = clockDriverSupplier;
        this.taxDriverSupplier = taxDriverSupplier;
    }

    @Override
    public void close() {
        shops.values().forEach(Closer::close);

        if (erp != null) {
            Closer.close(erp);
        } else {
            Closer.close(erpDriver);
        }

        if (clock != null) {
            Closer.close(clock);
        } else {
            Closer.close(clockDriver);
        }

        if (tax != null) {
            Closer.close(tax);
        } else {
            Closer.close(taxDriver);
        }
    }

    public ShopDsl shop(ChannelMode mode) {
        var channel = resolveShopChannel(mode);
        return getOrCreateShop(channel);
    }

    public ShopDsl shop() {
        return shop(channelMode);
    }

    private ShopDsl getOrCreateShop(String channel) {
        return shops.computeIfAbsent(channel, ch ->
                new ShopDsl(shopDriverFactory.apply(ch), context));
    }

    private String resolveShopChannel(ChannelMode mode) {
        String channel;
        if (mode == ChannelMode.STATIC) {
            channel = STATIC_CHANNEL;
        } else if (mode == ChannelMode.DYNAMIC) {
            channel = ChannelContext.get();
        } else {
            throw new IllegalStateException("Unknown channel mode: " + mode);
        }
        log.info("[ChannelMode] mode={} → channel={}", mode, channel);
        return channel;
    }

    public ErpDsl erp() {
        return getOrCreate(erp, () -> {
            erpDriver = getOrCreate(erpDriver, erpDriverSupplier);
            erp = new ErpDsl(erpDriver, context);
            return erp;
        });
    }

    public ClockDsl clock() {
        return getOrCreate(clock, () -> {
            clockDriver = getOrCreate(clockDriver, clockDriverSupplier);
            clock = new ClockDsl(clockDriver, context);
            return clock;
        });
    }

    public TaxDsl tax() {
        return getOrCreate(tax, () -> {
            taxDriver = getOrCreate(taxDriver, taxDriverSupplier);
            tax = new TaxDsl(taxDriver, context);
            return tax;
        });
    }

    private static <T> T getOrCreate(T instance, Supplier<T> supplier) {
        return instance != null ? instance : supplier.get();
    }
}
