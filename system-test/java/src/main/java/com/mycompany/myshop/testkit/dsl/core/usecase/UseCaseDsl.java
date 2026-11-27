package com.mycompany.myshop.testkit.dsl.core.usecase;

import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.clock.ClockDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.ErpDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.identity.IdentityProviderDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.tax.TaxDsl;
import com.mycompany.myshop.testkit.channel.ChannelType;
import com.mycompany.myshop.testkit.driver.port.external.clock.ClockDriver;
import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.driver.port.external.tax.TaxDriver;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.UserAccount;
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
    private static final String ADMIN_FIRST_NAME = "Test";
    private static final String ADMIN_LAST_NAME = "Admin";

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
    private UserIdentity admin;

    private ErpDsl erp;
    private ClockDsl clock;
    private TaxDsl tax;
    private IdentityProviderDsl identityProvider;

    public UseCaseDsl(ExternalSystemMode externalSystemMode,
                     ChannelMode channelMode,
                     Function<String, MyShopDriver> myShopDriverFactory,
                     Supplier<ErpDriver> erpDriverSupplier,
                     Supplier<ClockDriver> clockDriverSupplier,
                     Supplier<TaxDriver> taxDriverSupplier) {
        this(externalSystemMode, channelMode, myShopDriverFactory, erpDriverSupplier, clockDriverSupplier,
                taxDriverSupplier, () -> {
                    throw new IllegalStateException("This DSL was built without an identity provider, so it cannot"
                            + " create a new customer or admin");
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

        // Last: the shops' browser sessions may still be signed in as a created customer or admin.
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

    /**
     * A visitor signs up through the shop (UC-013, the UI channel only): the identity provider's registration form
     * creates the account, with this name and shipping country. It is removed again when this DSL is closed.
     *
     * @param email null for a unique address
     */
    public UserIdentity signedUpCustomer(String firstName, String lastName, String email, String country) {
        identityProviderDriver = getOrCreate(identityProviderDriver, identityProviderDriverSupplier);
        var account = identityProviderDriver.newAccountCredentials();
        try {
            myShop(ChannelMode.DYNAMIC).signUp()
                    .account(account.username(), account.password())
                    .email(email != null ? email : identityProviderDriver.newAccountEmail(account))
                    .name(firstName, lastName)
                    .country(country)
                    .execute()
                    .shouldSucceed();
        } finally {
            removeOnClose(account);
        }
        return UserIdentity.customer(account);
    }

    /** Also when the form was submitted but the shop never showed the customer signed in; no account, nothing to do. */
    private void removeOnClose(UserAccount account) {
        try {
            identityProviderDriver.deleteOnClose(account.username());
        } catch (IllegalStateException e) {
            log.info("No account {} to remove after the sign-up: {}", account.username(), e.getMessage());
        }
    }

    /**
     * Creates an admin account of this scenario's own (admin role), removed again when this DSL is closed.
     *
     * @param email null for a unique address
     */
    public UserIdentity newAdmin(String firstName, String lastName, String email) {
        identityProviderDriver = getOrCreate(identityProviderDriver, identityProviderDriverSupplier);
        return UserIdentity.admin(identityProviderDriver.createAdmin(firstName, lastName, email));
    }

    /**
     * This scenario's admin, created the first time it is needed: who admin-only operations run as, unless another
     * identity is selected.
     */
    public UserIdentity admin() {
        if (admin == null) {
            admin = newAdmin(ADMIN_FIRST_NAME, ADMIN_LAST_NAME, null);
        }
        return admin;
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
        return shops.computeIfAbsent(channel, ch -> {
            var driver = myShopDriverFactory.apply(ch);
            driver.useScenarioAdmin(this::admin);
            return new MyShopDsl(driver, context);
        });
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

    public IdentityProviderDsl identityProvider() {
        return getOrCreate(identityProvider, () -> {
            identityProviderDriver = getOrCreate(identityProviderDriver, identityProviderDriverSupplier);
            identityProvider = new IdentityProviderDsl(identityProviderDriver, context);
            return identityProvider;
        });
    }

    private static <T> T getOrCreate(T instance, Supplier<T> supplier) {
        return instance != null ? instance : supplier.get();
    }
}
