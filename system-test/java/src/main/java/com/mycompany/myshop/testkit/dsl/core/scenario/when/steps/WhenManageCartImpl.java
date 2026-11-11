package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import com.mycompany.myshop.testkit.driver.port.dtos.ManageCartResponse;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.StagedProducts;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.ManageCart;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.ManageCartVerification;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenManageCart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class WhenManageCartImpl extends BaseWhenStep<ManageCartResponse, ManageCartVerification> implements WhenManageCart {
    private final StagedProducts stagedProducts;
    private final List<CartLine> cartLines = new ArrayList<>();
    private String changedPriceSku;
    private String changedPrice;
    private boolean erpUnavailable;
    private boolean viewedWhileErpUnavailable;
    private UnaryOperator<ManageCart> action = ManageCart::viewCart;

    public WhenManageCartImpl(UseCaseDsl app, ScenarioDslImpl scenario, StagedProducts stagedProducts) {
        super(app, scenario);
        this.stagedProducts = stagedProducts;
    }

    @Override
    public WhenManageCartImpl withCartContaining(String skuAlias, int quantity) {
        cartLines.add(new CartLine(skuAlias, String.valueOf(quantity)));
        return this;
    }

    @Override
    public WhenManageCartImpl withErpPriceChanged(String skuAlias, String unitPrice) {
        this.changedPriceSku = skuAlias;
        this.changedPrice = unitPrice;
        return this;
    }

    @Override
    public WhenManageCartImpl withErpUnavailable() {
        this.erpUnavailable = true;
        return this;
    }

    @Override
    public WhenManageCartImpl withCartViewedWhileErpUnavailable() {
        this.viewedWhileErpUnavailable = true;
        return this;
    }

    @Override
    public WhenManageCartImpl withCatalogBrowsed() {
        return action(ManageCart::browseCatalog);
    }

    @Override
    public WhenManageCartImpl withAddedFromCatalog(String skuAlias) {
        return action(manageCart -> manageCart.addFromCatalog(skuAlias));
    }

    @Override
    public WhenManageCartImpl withAddedFromProductPage(String skuAlias, String quantity) {
        return action(manageCart -> manageCart.addFromProductPage(skuAlias, quantity));
    }

    @Override
    public WhenManageCartImpl withQuantityChanged(String skuAlias, String quantity) {
        return action(manageCart -> manageCart.changeQuantity(skuAlias, quantity));
    }

    @Override
    public WhenManageCartImpl withQuantityIncreased(String skuAlias) {
        return action(manageCart -> manageCart.increaseQuantity(skuAlias));
    }

    @Override
    public WhenManageCartImpl withQuantityDecreased(String skuAlias) {
        return action(manageCart -> manageCart.decreaseQuantity(skuAlias));
    }

    @Override
    public WhenManageCartImpl withLineRemoved(String skuAlias) {
        return action(manageCart -> manageCart.removeLine(skuAlias));
    }

    @Override
    public WhenManageCartImpl withSiteLeftAndRevisited() {
        return action(ManageCart::revisit);
    }

    @Override
    public WhenManageCartImpl withSignedInAsCustomer() {
        return action(ManageCart::signIn);
    }

    private WhenManageCartImpl action(UnaryOperator<ManageCart> action) {
        this.action = action;
        return this;
    }

    /** When the ERP changes after the cart is filled, filling the cart and the action are separate calls in one session. */
    @Override
    protected ExecutionResult<ManageCartResponse, ManageCartVerification> execute(UseCaseDsl app) {
        var erpChanges = changedPriceSku != null || erpUnavailable || viewedWhileErpUnavailable;
        var manageCart = app.myShop(ChannelMode.DYNAMIC).manageCart();
        if (erpChanges) {
            var fillCart = app.myShop(ChannelMode.DYNAMIC).manageCart();
            cartLines.forEach(line -> fillCart.cartLine(line.skuAlias(), line.quantity()));
            fillCart.execute().shouldSucceed();
            changeErp(app);
            manageCart.continueSession();
        } else {
            cartLines.forEach(line -> manageCart.cartLine(line.skuAlias(), line.quantity()));
        }
        return new ExecutionResultBuilder<>(action.apply(manageCart).execute()).build();
    }

    private void changeErp(UseCaseDsl app) {
        if (changedPriceSku != null) {
            stagedProducts.changePrice(changedPriceSku, changedPrice);
        }
        if (viewedWhileErpUnavailable) {
            app.erp().returnsUnavailable().execute().shouldSucceed();
            app.myShop(ChannelMode.DYNAMIC).manageCart().continueSession().viewCart().execute().shouldSucceed();
            app.erp().returnsAvailable().execute().shouldSucceed();
        }
        if (erpUnavailable) {
            app.erp().returnsUnavailable().execute().shouldSucceed();
        }
    }

    private record CartLine(String skuAlias, String quantity) {
    }
}
