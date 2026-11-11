package com.mycompany.myshop.testkit.dsl.core.scenario.when;

import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.DefaultGivens;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.CustomerResolver;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.StagedProducts;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenBrowseCatalogImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenViewProductImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenManageCartImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenCheckOutImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenBrowseCouponsImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenBrowseOrderHistoryImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenCancelOrderImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenDeliverOrderImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenPlaceOrderImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenPublishCouponImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.BaseWhenStep;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenViewOrderImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenUpdateProfileImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.when.steps.WhenViewProfileImpl;
import com.mycompany.myshop.testkit.dsl.port.when.WhenStage;

public class WhenImpl implements WhenStage {
    private final UseCaseDsl app;
    private final ScenarioDslImpl scenario;
    private final DefaultGivens defaults;
    private final CustomerResolver customers;
    private final StagedProducts stagedProducts;

    public WhenImpl(UseCaseDsl app, ScenarioDslImpl scenario, DefaultGivens defaults, CustomerResolver customers,
                    StagedProducts stagedProducts) {
        this.app = app;
        this.scenario = scenario;
        this.defaults = defaults;
        this.customers = customers;
        this.stagedProducts = stagedProducts;
    }

    public WhenImpl(UseCaseDsl app, ScenarioDslImpl scenario, DefaultGivens defaults, CustomerResolver customers) {
        this(app, scenario, defaults, customers, StagedProducts.NONE);
    }

    public WhenImpl(UseCaseDsl app, ScenarioDslImpl scenario, DefaultGivens defaults) {
        this(app, scenario, defaults, CustomerResolver.standalone());
    }

    public WhenImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        this(app, scenario, new DefaultGivens(app));
    }

    private void ensureDefaults() {
        defaults.ensure(true);
    }

    /** Profile steps don't touch the ERP or tax service, so only the givens' own dependents are created. */
    private void ensureDefaultsForProfile() {
        defaults.ensure(false);
    }

    public WhenPlaceOrderImpl placeOrder() {
        ensureDefaults();
        return new WhenPlaceOrderImpl(app, scenario);
    }

    public WhenCancelOrderImpl cancelOrder() {
        ensureDefaults();
        return new WhenCancelOrderImpl(app, scenario);
    }

    public WhenDeliverOrderImpl deliverOrder() {
        ensureDefaults();
        return new WhenDeliverOrderImpl(app, scenario);
    }

    public WhenViewOrderImpl viewOrder() {
        ensureDefaults();
        return new WhenViewOrderImpl(app, scenario);
    }

    public WhenBrowseOrderHistoryImpl browseOrderHistory() {
        ensureDefaults();
        return new WhenBrowseOrderHistoryImpl(app, scenario, customers);
    }

    public WhenPublishCouponImpl publishCoupon() {
        ensureDefaults();
        return new WhenPublishCouponImpl(app, scenario);
    }

    public WhenBrowseCouponsImpl browseCoupons() {
        ensureDefaults();
        return new WhenBrowseCouponsImpl(app, scenario);
    }

    public WhenViewProfileImpl viewProfile() {
        ensureDefaultsForProfile();
        return new WhenViewProfileImpl(app, scenario);
    }

    public WhenUpdateProfileImpl updateProfile() {
        ensureDefaultsForProfile();
        return new WhenUpdateProfileImpl(app, scenario);
    }

    /** The catalog lists only the products the scenario staged, so no default product or tax rate is created. */
    public WhenBrowseCatalogImpl browseCatalog() {
        defaults.ensure(false);
        return new WhenBrowseCatalogImpl(app, scenario);
    }

    /** The product page shows only the products the scenario staged, so no default product or tax rate is created. */
    public WhenViewProductImpl viewProduct() {
        defaults.ensure(false);
        return new WhenViewProductImpl(app, scenario);
    }

    /** The cart shows only the products the scenario staged, so no default product or tax rate is created. */
    public WhenManageCartImpl manageCart() {
        defaults.ensure(false);
        return new WhenManageCartImpl(app, scenario, stagedProducts);
    }

    /** The checkout shows only the products the scenario staged, so no default product or tax rate is created. */
    public WhenCheckOutImpl checkOut() {
        defaults.ensure(false);
        return new WhenCheckOutImpl(app, scenario, stagedProducts);
    }

}
