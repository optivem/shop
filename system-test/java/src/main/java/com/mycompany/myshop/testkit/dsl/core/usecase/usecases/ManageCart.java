package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.ManageCartRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ManageCartResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;

import java.util.ArrayList;
import java.util.List;

/** SKUs are aliases, resolved to the values the ERP was given. Without an action, only the cart is filled. */
public class ManageCart extends BaseMyShopUseCase<ManageCartResponse, ManageCartVerification> {
    private final List<CartLineEntry> cartLines = new ArrayList<>();
    private Boolean continueSession;
    private String action;
    private String skuAlias;
    private String quantity;

    public ManageCart(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    /** A line the cart gets first, added from its product page; lines are added in the order given. */
    public ManageCart cartLine(String skuAlias, String quantity) {
        cartLines.add(new CartLineEntry(skuAlias, quantity));
        return this;
    }

    /** Keeps the browser session of the previous call instead of starting a new one. */
    public ManageCart continueSession() {
        this.continueSession = true;
        return this;
    }

    public ManageCart viewCart() {
        return action(ManageCartRequest.VIEW_CART, null, null);
    }

    public ManageCart browseCatalog() {
        return action(ManageCartRequest.BROWSE_CATALOG, null, null);
    }

    public ManageCart addFromCatalog(String skuAlias) {
        return action(ManageCartRequest.ADD_FROM_CATALOG, skuAlias, null);
    }

    public ManageCart addFromProductPage(String skuAlias, String quantity) {
        return action(ManageCartRequest.ADD_FROM_PRODUCT_PAGE, skuAlias, quantity);
    }

    public ManageCart changeQuantity(String skuAlias, String quantity) {
        return action(ManageCartRequest.CHANGE_QUANTITY, skuAlias, quantity);
    }

    public ManageCart increaseQuantity(String skuAlias) {
        return action(ManageCartRequest.INCREASE_QUANTITY, skuAlias, null);
    }

    public ManageCart decreaseQuantity(String skuAlias) {
        return action(ManageCartRequest.DECREASE_QUANTITY, skuAlias, null);
    }

    public ManageCart removeLine(String skuAlias) {
        return action(ManageCartRequest.REMOVE_LINE, skuAlias, null);
    }

    public ManageCart revisit() {
        return action(ManageCartRequest.REVISIT, null, null);
    }

    public ManageCart signIn() {
        return action(ManageCartRequest.SIGN_IN, null, null);
    }

    private ManageCart action(String action, String skuAlias, String quantity) {
        this.action = action;
        this.skuAlias = skuAlias;
        this.quantity = quantity;
        return this;
    }

    @Override
    public UseCaseResult<ManageCartResponse, ManageCartVerification> execute() {
        var lines = cartLines.stream()
                .map(line -> ManageCartRequest.CartLineDto.builder()
                        .sku(context.getParamValue(line.skuAlias()))
                        .quantity(line.quantity())
                        .build())
                .toList();
        var request = ManageCartRequest.builder()
                .cartLines(lines)
                .continueSession(continueSession)
                .action(action)
                .sku(skuAlias == null ? null : context.getParamValue(skuAlias))
                .quantity(quantity)
                .build();
        var result = driver.manageCart(request);

        return new UseCaseResult<>(result, context, ManageCartVerification::new);
    }

    private record CartLineEntry(String skuAlias, String quantity) {
    }
}
