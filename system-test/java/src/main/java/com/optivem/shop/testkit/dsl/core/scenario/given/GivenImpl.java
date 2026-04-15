package com.optivem.shop.testkit.dsl.core.scenario.given;

import com.optivem.shop.testkit.dsl.core.usecase.UseCaseDsl;
import com.optivem.shop.testkit.dsl.core.scenario.then.ThenImpl;
import com.optivem.shop.testkit.dsl.core.scenario.given.steps.GivenClockImpl;
import com.optivem.shop.testkit.dsl.core.scenario.given.steps.GivenCouponImpl;
import com.optivem.shop.testkit.dsl.core.scenario.given.steps.GivenCountryImpl;
import com.optivem.shop.testkit.dsl.core.scenario.given.steps.GivenOrderImpl;
import com.optivem.shop.testkit.dsl.core.scenario.given.steps.GivenProductImpl;
import com.optivem.shop.testkit.dsl.core.scenario.given.steps.GivenPromotionImpl;
import com.optivem.shop.testkit.dsl.port.given.GivenStage;
import com.optivem.shop.testkit.dsl.port.then.ThenStage;
import com.optivem.shop.testkit.dsl.core.scenario.when.WhenImpl;

import java.util.ArrayList;
import java.util.List;

public class GivenImpl implements GivenStage {
    private final UseCaseDsl app;
    private GivenClockImpl clock;
    private GivenPromotionImpl promotion;
    private final List<GivenProductImpl> products;
    private final List<GivenOrderImpl> orders;
    private final List<GivenCountryImpl> countries;
    private final List<GivenCouponImpl> coupons;

    public GivenImpl(UseCaseDsl app) {
        this.app = app;
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

    public WhenImpl when() {
        setup();
        return new WhenImpl(app, !products.isEmpty(), !countries.isEmpty(), true);
    }

    public ThenStage then() {
        setup();
        return new ThenImpl(app);
    }

    private void setup() {
        setupClock();
        setupErp();
        setupTax();
        setupPromotion();
        setupShop();
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

    private void setupShop() {
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
