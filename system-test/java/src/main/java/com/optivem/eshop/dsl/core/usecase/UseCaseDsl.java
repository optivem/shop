package com.optivem.eshop.dsl.core.usecase;

import com.optivem.eshop.dsl.driver.port.external.clock.ClockDriver;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;
import com.optivem.eshop.dsl.core.usecase.external.clock.ClockDsl;
import com.optivem.eshop.dsl.driver.port.external.erp.ErpDriver;
import com.optivem.eshop.dsl.core.usecase.external.erp.ErpDsl;
import com.optivem.eshop.dsl.driver.port.shop.ShopDriver;
import com.optivem.eshop.dsl.core.usecase.shop.ShopDsl;
import com.optivem.eshop.dsl.port.ExternalSystemMode;
import com.optivem.eshop.dsl.common.Closer;

import java.io.Closeable;
import java.util.function.Supplier;

public class UseCaseDsl implements Closeable {
    private final UseCaseContext context;
    private final Supplier<ShopDriver> shopDriverSupplier;
    private final Supplier<ErpDriver> erpDriverSupplier;
    private final Supplier<ClockDriver> clockDriverSupplier;

    private ShopDriver shopDriver;
    private ErpDriver erpDriver;
    private ClockDriver clockDriver;

    private ShopDsl shop;
    private ErpDsl erp;
    private ClockDsl clock;

    private UseCaseDsl(UseCaseContext context,
                      Supplier<ShopDriver> shopDriverSupplier,
                      Supplier<ErpDriver> erpDriverSupplier,
                      Supplier<ClockDriver> clockDriverSupplier) {
        this.context = context;
        this.shopDriverSupplier = shopDriverSupplier;
        this.erpDriverSupplier = erpDriverSupplier;
        this.clockDriverSupplier = clockDriverSupplier;
    }

    public UseCaseDsl(ExternalSystemMode externalSystemMode, ShopDriver shopDriver, ErpDriver erpDriver, ClockDriver clockDriver) {
        this(
                externalSystemMode,
                () -> shopDriver,
                () -> erpDriver,
                () -> clockDriver
        );
    }

    public UseCaseDsl(ExternalSystemMode externalSystemMode,
                     Supplier<ShopDriver> shopDriverSupplier,
                     Supplier<ErpDriver> erpDriverSupplier,
                     Supplier<ClockDriver> clockDriverSupplier) {
        this(new UseCaseContext(externalSystemMode), shopDriverSupplier, erpDriverSupplier, clockDriverSupplier);
    }

    @Override
    public void close() {
        if (shop != null) {
            Closer.close(shop);
        } else {
            Closer.close(shopDriver);
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
    }

    public ShopDsl shop() {
        return getOrCreate(shop, () -> {
            shopDriver = getOrCreate(shopDriver, shopDriverSupplier);
            shop = new ShopDsl(shopDriver, context);
            return shop;
        });
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

    private static <T> T getOrCreate(T instance, Supplier<T> supplier) {
        return instance != null ? instance : supplier.get();
    }
}



