package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductResponse;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.ViewProductVerification;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenViewProduct;

public class WhenViewProductImpl extends BaseWhenStep<ViewProductResponse, ViewProductVerification> implements WhenViewProduct {
    private String sku;
    private boolean fromCatalog;
    private String cartSku;
    private String cartQuantity;
    private String quantity;
    private boolean increase;
    private boolean decrease;
    private boolean addToCart;
    private boolean backToCatalog;

    public WhenViewProductImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
    }

    @Override
    public WhenViewProductImpl withSku(String skuAlias) {
        this.sku = skuAlias;
        return this;
    }

    @Override
    public WhenViewProductImpl fromCatalog() {
        this.fromCatalog = true;
        return this;
    }

    @Override
    public WhenViewProductImpl withCartContaining(String skuAlias, int quantity) {
        this.cartSku = skuAlias;
        this.cartQuantity = String.valueOf(quantity);
        return this;
    }

    @Override
    public WhenViewProductImpl withQuantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    @Override
    public WhenViewProductImpl withQuantityIncreased() {
        this.increase = true;
        return this;
    }

    @Override
    public WhenViewProductImpl withQuantityDecreased() {
        this.decrease = true;
        return this;
    }

    @Override
    public WhenViewProductImpl withAddedToCart(String quantity) {
        this.quantity = quantity;
        this.addToCart = true;
        return this;
    }

    @Override
    public WhenViewProductImpl withBackToCatalogFollowed() {
        this.backToCatalog = true;
        return this;
    }

    @Override
    protected ExecutionResult<ViewProductResponse, ViewProductVerification> execute(UseCaseDsl app) {
        var viewProduct = app.myShop(ChannelMode.DYNAMIC).viewProduct()
                .sku(sku)
                .quantity(quantity);
        if (fromCatalog) {
            viewProduct.fromCatalog();
        }
        if (cartSku != null) {
            viewProduct.cartLine(cartSku, cartQuantity);
        }
        if (increase) {
            viewProduct.increaseQuantity();
        }
        if (decrease) {
            viewProduct.decreaseQuantity();
        }
        if (addToCart) {
            viewProduct.addToCart();
        }
        if (backToCatalog) {
            viewProduct.backToCatalog();
        }
        return new ExecutionResultBuilder<>(viewProduct.execute()).build();
    }
}
