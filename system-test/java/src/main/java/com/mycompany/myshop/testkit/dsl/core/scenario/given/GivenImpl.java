package com.mycompany.myshop.testkit.dsl.core.scenario.given;

import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.DefaultGivens;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.then.ThenImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenClockImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenCouponImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenCountryImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenErpImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenOrderImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenProductImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenProductsImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenPromotionImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenProfileImpl;
import com.mycompany.myshop.testkit.dsl.port.given.GivenStage;
import com.mycompany.myshop.testkit.dsl.port.then.ThenStage;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.WhenImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class GivenImpl implements GivenStage {
    private final UseCaseDsl app;
    private final ScenarioDslImpl scenario;
    private GivenClockImpl clock;
    private GivenErpImpl erp;
    private GivenPromotionImpl promotion;
    private final List<GivenProductImpl> products;
    private final List<GivenProductsImpl> productLists;
    private final List<GivenOrderImpl> orders;
    private final List<GivenCountryImpl> countries;
    private final List<GivenCouponImpl> coupons;
    private final List<GivenProfileImpl> profiles;
    private final DefaultGivens defaults;
    private final CustomerAliases customers = new CustomerAliases();
    private Supplier<UserIdentity> loggedIn = () -> UserIdentity.DEFAULT;
    private boolean loggedInChosen;

    public GivenImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        this.app = app;
        this.scenario = scenario;
        this.defaults = new DefaultGivens(app);
        this.clock = null;
        this.promotion = new GivenPromotionImpl(this);
        this.products = new ArrayList<>();
        this.productLists = new ArrayList<>();
        this.orders = new ArrayList<>();
        this.countries = new ArrayList<>();
        this.coupons = new ArrayList<>();
        this.profiles = new ArrayList<>();
    }

    @Override
    public GivenErpImpl erp() {
        erp = new GivenErpImpl(this);
        return erp;
    }

    public GivenProductImpl product() {
        defaults.markProduct();
        var product = new GivenProductImpl(this);
        products.add(product);
        return product;
    }

    /** Stages the ERP product list; it replaces the default product, so a list scenario sees only what it stages. */
    public GivenProductsImpl products() {
        defaults.markProduct();
        var productList = new GivenProductsImpl(this);
        productLists.add(productList);
        return productList;
    }

    public GivenOrderImpl order() {
        defaults.markOrder();
        var order = new GivenOrderImpl(this);
        orders.add(order);
        return order;
    }

    public GivenClockImpl clock() {
        clock = new GivenClockImpl(this);
        return clock;
    }

    @Override
    public GivenPromotionImpl promotion() {
        promotion = new GivenPromotionImpl(this);
        return promotion;
    }

    public GivenCountryImpl country() {
        defaults.markTaxRate();
        var country = new GivenCountryImpl(this);
        countries.add(country);
        return country;
    }

    public GivenCouponImpl coupon() {
        defaults.markCoupon();
        var coupon = new GivenCouponImpl(this);
        coupons.add(coupon);
        return coupon;
    }

    public GivenProfileImpl profile() {
        var profile = new GivenProfileImpl(this);
        profiles.add(profile);
        return profile;
    }

    @Override
    public GivenImpl loggedInAsCustomer() {
        customers.reserveDefaultCustomer();
        return loggedInAs(customers::defaultCustomer);
    }

    @Override
    public GivenImpl loggedInAsCustomer(String alias) {
        customers.register(alias);
        return loggedInAs(() -> customers.resolve(alias));
    }

    @Override
    public GivenImpl loggedInAsAdmin() {
        return loggedInAs(() -> UserIdentity.ADMIN);
    }

    @Override
    public GivenImpl notLoggedIn() {
        return loggedInAs(() -> UserIdentity.ANONYMOUS);
    }

    private GivenImpl loggedInAs(Supplier<UserIdentity> identity) {
        this.loggedIn = identity;
        this.loggedInChosen = true;
        return this;
    }

    /** Registers a customer alias for an order placed in this scenario. */
    public void registerCustomer(String alias) {
        customers.register(alias);
    }

    /** Resolves the customer behind an alias; only valid once the scenario is being set up. */
    public UserIdentity resolveCustomer(String alias) {
        return customers.resolve(alias);
    }

    /** The default customer (customer1). */
    public UserIdentity defaultCustomer() {
        return customers.defaultCustomer();
    }

    public WhenImpl when() {
        setup();
        return new WhenImpl(app, scenario, defaults, customerResolver(), stagedProducts());
    }

    private StagedProducts stagedProducts() {
        return new StagedProducts() {
            @Override
            public void changePrice(String skuAlias, String unitPrice) {
                changeProductPrice(skuAlias, unitPrice);
            }

            @Override
            public void withdraw(String skuAlias) {
                withdrawProduct(skuAlias);
            }
        };
    }

    private void withdrawProduct(String skuAlias) {
        if (productLists.isEmpty()) {
            StagedProducts.NONE.withdraw(skuAlias);
        }
        for (var productList : productLists) {
            productList.executeWithout(app, skuAlias);
        }
        app.erp().returnsProductNotFound().sku(skuAlias).execute().shouldSucceed();
    }

    private void changeProductPrice(String skuAlias, String unitPrice) {
        if (productLists.isEmpty()) {
            StagedProducts.NONE.changePrice(skuAlias, unitPrice);
        }
        for (var productList : productLists) {
            productList.executeWithPrice(app, skuAlias, unitPrice);
        }
    }

    /** Resolves aliases named in the when stage (e.g. a customer filter) against this scenario's customers. */
    private CustomerResolver customerResolver() {
        return new CustomerResolver() {
            @Override
            public UserIdentity defaultCustomer() {
                return GivenImpl.this.defaultCustomer();
            }

            @Override
            public UserIdentity customer(String alias) {
                registerCustomer(alias);
                return resolveCustomer(alias);
            }
        };
    }

    public ThenStage then() {
        setup();
        return new ThenImpl(app);
    }

    private void setup() {
        reserveDefaultCustomerIfUsed();
        setupClock();
        setupErp();
        setupTax();
        setupPromotion();
        setupMyShop();
        app.actAs(loggedIn.get());
    }

    /** Operations with no explicit customer run as the default customer, so customer1 must stay theirs. */
    private void reserveDefaultCustomerIfUsed() {
        if (!loggedInChosen || orders.stream().anyMatch(GivenOrderImpl::isPlacedByDefaultCustomer)
                || profiles.stream().anyMatch(GivenProfileImpl::isSavedByDefaultCustomer)) {
            customers.reserveDefaultCustomer();
        }
    }

    private void setupPromotion() {
        promotion.execute(app);
        defaults.markPromotion();
    }

    private void setupClock() {
        if (clock != null) {
            clock.execute(app);
        }
    }

    private void setupErp() {
        for (var product : products) {
            product.execute(app);
        }
        for (var productList : productLists) {
            productList.execute(app);
        }
        if (erp != null) {
            erp.execute(app);
        }
    }

    private void setupTax() {
        for (var country : countries) {
            country.execute(app);
        }
    }

    private void setupMyShop() {
        setupCoupons();
        setupOrders();
        setupProfiles();
    }

    private void setupProfiles() {
        for (var profile : profiles) {
            profile.execute(app);
        }
    }

    private void setupCoupons() {
        defaults.ensure(false);

        for (var coupon : coupons) {
            coupon.execute(app);
        }
    }

    private void setupOrders() {
        for (var order : orders) {
            order.execute(app);
        }
    }
}
