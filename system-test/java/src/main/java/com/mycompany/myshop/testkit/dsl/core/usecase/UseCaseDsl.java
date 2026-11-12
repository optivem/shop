package com.mycompany.myshop.testkit.dsl.core.usecase;

import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.clock.ClockDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.ErpDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.tax.TaxDsl;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.driver.port.external.tax.TaxDriver;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.driver.port.external.identity.IdentityProviderDriver;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.ExternalSystemMode;
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
    private final Function<String, MyShopDriver> myShopDriverFactory;
    private final Supplier<ErpDriver> erpDriverSupplier;
    private final Supplier<ClockDriver> clockDriverSupplier;
    private final Supplier<TaxDriver> taxDriverSupplier;
    private final Supplier<IdentityProviderDriver> identityProviderDriverSupplier;

    private final Map<String, MyShopDsl> shops = new HashMap<>();
    private UserIdentity identity = UserIdentity.DEFAULT;
    private ErpDriver erpDriver;
    private ClockDriver clockDriver;
    private TaxDriver taxDriver;
    private IdentityProviderDriver identityProviderDriver;

    private ErpDsl erp;
    private ClockDsl clock;
    private TaxDsl tax;

    public UseCaseDsl(ExternalSystemMode externalSystemMode,
                     ChannelMode channelMode,
                     Function<String, MyShopDriver> myShopDriverFactory,
                     Supplier<ErpDriver> erpDriverSupplier,
                     Supplier<ClockDriver> clockDriverSupplier,
                     Supplier<TaxDriver> taxDriverSupplier) {
        this(externalSystemMode, channelMode, myShopDriverFactory, erpDriverSupplier, clockDriverSupplier,
                taxDriverSupplier, () -> {
                    throw new IllegalStateException("This DSL was built without an identity provider, so it cannot"
                            + " create a new customer");
                });
    }

    public UseCaseDsl(ExternalSystemMode externalSystemMode,
                     ChannelMode channelMode,
                     Function<String, MyShopDriver> myShopDriverFactory,
                     Supplier<ErpDriver> erpDriverSupplier,
                     Supplier<ClockDriver> clockDriverSupplier,
                     Supplier<TaxDriver> taxDriverSupplier,
                     Supplier<IdentityProviderDriver> identityProviderDriverSupplier) {
        this.context = new UseCaseContext(externalSystemMode);
        this.channelMode = channelMode;
        this.myShopDriverFactory = myShopDriverFactory;
        this.erpDriverSupplier = erpDriverSupplier;
        this.clockDriverSupplier = clockDriverSupplier;
        this.taxDriverSupplier = taxDriverSupplier;
        this.identityProviderDriverSupplier = identityProviderDriverSupplier;
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

        // Last: the shops' browser sessions may still be signed in as a created customer.
        Closer.close(identityProviderDriver);
    }

    /**
     * Creates a customer account of this scenario's own (customer role, no saved profile, no orders), removed again
     * when this DSL is closed.
     *
     * @param email null for a unique address
     */
    public UserIdentity newCustomer(String firstName, String lastName, String email) {
        identityProviderDriver = getOrCreate(identityProviderDriver, identityProviderDriverSupplier);
        return UserIdentity.customer(identityProviderDriver.createCustomer(firstName, lastName, email));
    }

    /** Selects who MyShop operations are performed as, until changed again. */
    public void actAs(UserIdentity identity) {
        this.identity = identity;
    }

    /** Runs {@code action} as {@code identity}, then acts as before again. */
    public <T> T actingAs(UserIdentity identity, Supplier<T> action) {
        var previous = this.identity;
        actAs(identity);
        try {
            return action.get();
        } finally {
            actAs(previous);
        }
    }

    public MyShopDsl myShop(ChannelMode mode) {
        var channel = resolveMyShopChannel(mode);
        var shop = getOrCreateMyShop(channel);
        shop.actAs(identity);
        return shop;
    }

    public MyShopDsl myShop() {
        return myShop(channelMode);
    }

    private MyShopDsl getOrCreateMyShop(String channel) {
        return shops.computeIfAbsent(channel, ch ->
                new MyShopDsl(myShopDriverFactory.apply(ch), context));
    }

    private String resolveMyShopChannel(ChannelMode mode) {
        String channel = switch (mode) {
            case STATIC -> STATIC_CHANNEL;
            case DYNAMIC -> ChannelContext.get();
            default -> throw new IllegalStateException("Unknown channel mode: " + mode);
        };
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
