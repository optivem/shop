package com.optivem.shop.systemtest.legacy.mod03.e2e;

import com.optivem.shop.systemtest.legacy.mod03.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.optivem.shop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderNegativeUiTest extends BaseE2eTest {
    @Override
    protected void setShopClient() {
        setUpShopBrowser();
    }

    @Test
    void shouldRejectOrderWithNonIntegerQuantity() {
        shopUiPage.navigate(getShopUiBaseUrl());
        shopUiPage.locator("a[href='/new-order']").click();

        shopUiPage.locator("[aria-label=\"SKU\"]").fill(SKU + "-" + UUID.randomUUID().toString().substring(0, 8));
        shopUiPage.locator("[aria-label=\"Quantity\"]").fill("invalid-quantity");
        shopUiPage.locator("[aria-label=\"Place Order\"]").click();

        var errorAlert = shopUiPage.locator("[role='alert'][data-notification-id]");
        errorAlert.waitFor();
        assertThat(errorAlert.isVisible()).isTrue();
        var errorText = errorAlert.textContent();
        assertThat(errorText)
                .contains("The request contains one or more validation errors")
                .contains("quantity")
                .contains("Quantity must be an integer");
    }
}
