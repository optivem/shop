package com.optivem.shop.dsl.core.usecase;

import com.optivem.shop.dsl.common.Closer;
import com.optivem.shop.dsl.core.shared.UseCaseContext;
import com.optivem.shop.dsl.core.usecase.external.clock.ClockDsl;
import com.optivem.shop.dsl.core.usecase.external.erp.ErpDsl;
import com.optivem.shop.dsl.core.usecase.external.tax.TaxDsl;
import com.optivem.shop.dsl.core.usecase.shop.ShopDsl;
import com.optivem.shop.dsl.driver.port.external.clock.ClockDriver;
import com.optivem.shop.dsl.driver.port.external.erp.ErpDriver;
import com.optivem.shop.dsl.driver.port.external.tax.TaxDriver;
import com.optivem.shop.dsl.driver.port.shop.ShopDriver;
import com.optivem.shop.dsl.port.ChannelMode;
import com.optivem.shop.dsl.port.ExternalSystemMode;
import com.optivem.testing.contexts.ChannelContext;

import java.io.Closeable;
import java.util.function.Function;
import java.util.function.Supplier;

public class UseCaseDsl implements Closeable {
    private final UseCaseContext context;
    private final ChannelMode channelMode;
    private final String staticChannel;
    private final Function<String, ShopDriver> shopDriverFactory;
    private final Supplier<ErpDriver> erpDriverSupplier;
    private final Supplier<ClockDriver> clockDriverSupplier;
    private final Supplier<TaxDriver> taxDriverSupplier;

    private ShopDriver shopDriver;
    private ShopDriver dynamicShopDriver;
    private ErpDriver erpDriver;
    private ClockDriver clockDriver;
    private TaxDriver taxDriver;

    private ShopDsl shop;
    private ShopDsl dynamicShop;
    private ErpDsl erp;
    private ClockDsl clock;
    private TaxDsl tax;

    public UseCaseDsl(ExternalSystemMode externalSystemMode,
                     Function<String, ShopDriver> shopDriverFactory,
                     Supplier<ErpDriver> erpDriverSupplier,
                     Supplier<ClockDriver> clockDriverSupplier,
                     Supplier<TaxDriver> taxDriverSupplier) {
        this(externalSystemMode, ChannelMode.DYNAMIC, null, shopDriverFactory, erpDriverSupplier, clockDriverSupplier, taxDriverSupplier);
    }

    public UseCaseDsl(ExternalSystemMode externalSystemMode,
                     ChannelMode channelMode,
                     String staticChannel,
                     Function<String, ShopDriver> shopDriverFactory,
                     Supplier<ErpDriver> erpDriverSupplier,
                     Supplier<ClockDriver> clockDriverSupplier,
                     Supplier<TaxDriver> taxDriverSupplier) {
        this(new UseCaseContext(externalSystemMode), channelMode, staticChannel, shopDriverFactory, erpDriverSupplier, clockDriverSupplier, taxDriverSupplier);
    }

    private UseCaseDsl(UseCaseContext context,
                      ChannelMode channelMode,
                      String staticChannel,
                      Function<String, ShopDriver> shopDriverFactory,
                      Supplier<ErpDriver> erpDriverSupplier,
                      Supplier<ClockDriver> clockDriverSupplier,
                      Supplier<TaxDriver> taxDriverSupplier) {
        this.context = context;
        this.channelMode = channelMode;
        this.staticChannel = staticChannel;
        this.shopDriverFactory = shopDriverFactory;
        this.erpDriverSupplier = erpDriverSupplier;
        this.clockDriverSupplier = clockDriverSupplier;
        this.taxDriverSupplier = taxDriverSupplier;
    }

    @Override
    public void close() {
        if (shop != null) {
            Closer.close(shop);
        } else {
            Closer.close(shopDriver);
        }

        if (dynamicShop != null && dynamicShop != shop) {
            Closer.close(dynamicShop);
        } else if (dynamicShopDriver != null && dynamicShopDriver != shopDriver) {
            Closer.close(dynamicShopDriver);
        }

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

    public ShopDsl shop() {
        var channel = resolveShopChannel();
        return getOrCreate(shop, () -> {
            shopDriver = shopDriverFactory.apply(channel);
            shop = new ShopDsl(shopDriver, context);
            return shop;
        });
    }

    public ShopDsl shop(ChannelMode mode) {
        if (mode == ChannelMode.DYNAMIC) {
            var channel = ChannelContext.get();
            if (channel.equals(resolveShopChannel())) {
                return shop();
            }
            return getOrCreate(dynamicShop, () -> {
                dynamicShopDriver = shopDriverFactory.apply(channel);
                dynamicShop = new ShopDsl(dynamicShopDriver, context);
                return dynamicShop;
            });
        }
        return shop();
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

    private String resolveShopChannel() {
        if (channelMode == ChannelMode.STATIC) {
            return staticChannel;
        }
        return ChannelContext.get();
    }

    private static <T> T getOrCreate(T instance, Supplier<T> supplier) {
        return instance != null ? instance : supplier.get();
    }
}
