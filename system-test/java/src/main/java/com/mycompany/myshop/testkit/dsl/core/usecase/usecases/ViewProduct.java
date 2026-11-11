package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;

/** SKUs are aliases, resolved to the values the ERP was given. */
public class ViewProduct extends BaseMyShopUseCase<ViewProductResponse, ViewProductVerification> {
    private String skuAlias;
    private Boolean fromCatalog;
    private String cartSkuAlias;
    private String cartQuantity;
    private String quantity;
    private String quantityStep;
    private Boolean addToCart;
    private String followUp;

    public ViewProduct(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public ViewProduct sku(String skuAlias) {
        this.skuAlias = skuAlias;
        return this;
    }

    public ViewProduct fromCatalog() {
        this.fromCatalog = true;
        return this;
    }

    public ViewProduct cartLine(String skuAlias, String quantity) {
        this.cartSkuAlias = skuAlias;
        this.cartQuantity = quantity;
        return this;
    }

    public ViewProduct quantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    public ViewProduct increaseQuantity() {
        this.quantityStep = ViewProductRequest.INCREASE;
        return this;
    }

    public ViewProduct decreaseQuantity() {
        this.quantityStep = ViewProductRequest.DECREASE;
        return this;
    }

    public ViewProduct addToCart() {
        this.addToCart = true;
        return this;
    }

    public ViewProduct backToCatalog() {
        this.followUp = ViewProductRequest.BACK_TO_CATALOG;
        return this;
    }

    @Override
    public UseCaseResult<ViewProductResponse, ViewProductVerification> execute() {
        var request = ViewProductRequest.builder()
                .sku(context.getParamValue(skuAlias))
                .fromCatalog(fromCatalog)
                .cartSku(cartSkuAlias == null ? null : context.getParamValue(cartSkuAlias))
                .cartQuantity(cartQuantity)
                .quantity(quantity)
                .quantityStep(quantityStep)
                .addToCart(addToCart)
                .followUp(followUp)
                .build();
        var result = driver.viewProduct(request);

        return new UseCaseResult<>(result, context, ViewProductVerification::new);
    }
}
