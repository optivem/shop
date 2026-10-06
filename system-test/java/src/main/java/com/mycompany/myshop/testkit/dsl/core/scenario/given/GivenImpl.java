package com.mycompany.myshop.testkit.dsl.core.scenario.given;

import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.then.ThenImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenClockImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenCouponImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenCountryImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenOrderImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenProductImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.steps.GivenPromotionImpl;
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
    private GivenPromotionImpl promotion;
    private final List<GivenProductImpl> products;
    private final List<GivenOrderImpl> orders;
    private final List<GivenCountryImpl> countries;
    private final List<GivenCouponImpl> coupons;
    private final CustomerAliases customers = new CustomerAliases();
    private Supplier<UserIdentity> loggedIn = () -> UserIdentity.DEFAULT;
    private boolean loggedInChosen;

    public GivenImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        this.app = app;
        this.scenario = scenario;
        this.clock = null;
        this.promotion = new GivenPromotionImpl(this);
        this.products = new ArrayList<>();
        this.orders = new ArrayList<>();
        this.countries = new ArrayList<>();
        this.coupons = new ArrayList<>();
    }

    public GivenProductImpl product() {
        var product = new GivenProductImpl(this);
        products.add(product);
        return product;
    }

    public GivenOrderImpl order() {
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
        var country = new GivenCountryImpl(this);
        countries.add(country);
        return country;
    }

    public GivenCouponImpl coupon() {
        var coupon = new GivenCouponImpl(this);
        coupons.add(coupon);
        return coupon;
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
        return new WhenImpl(app, scenario, !products.isEmpty(), !countries.isEmpty(), true);
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
        if (!loggedInChosen || orders.stream().anyMatch(GivenOrderImpl::isPlacedByDefaultCustomer)) {
            customers.reserveDefaultCustomer();
        }
    }

    private void setupPromotion() {
        promotion.execute(app);
    }

    private void setupClock() {
        if (clock != null) {
            clock.execute(app);
        }
    }

    private void setupErp() {
        if (!orders.isEmpty() && products.isEmpty()) {
            var defaultProduct = new GivenProductImpl(this);
            products.add(defaultProduct);
        }

        for (var product : products) {
            product.execute(app);
        }
    }

    private void setupTax() {
        if (!orders.isEmpty() && countries.isEmpty()) {
            var defaultCountry = new GivenCountryImpl(this);
            countries.add(defaultCountry);
        }

        for (var country : countries) {
            country.execute(app);
        }
    }

    private void setupMyShop() {
        setupCoupons();
        setupOrders();
    }

    private void setupCoupons() {
        if (!orders.isEmpty() && coupons.isEmpty()) {
            var defaultCoupon = new GivenCouponImpl(this);
            coupons.add(defaultCoupon);
        }

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
