package com.mycompany.myshop.systemtest.legacy.mod03.e2e;

import com.mycompany.myshop.systemtest.legacy.mod03.e2e.base.BaseE2eTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.mycompany.myshop.systemtest.commons.constants.Defaults.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderNegativeUiTest extends BaseE2eTest {
    @Override
    protected void setMyShopClient() {
        setUpMyShopBrowser();
    }

    @Test
    void shouldRejectOrderWithNonIntegerQuantity() {
        myShopUiPage.navigate(getMyShopUiBaseUrl());
        loginToMyShopUiIfRequired();
        myShopUiPage.locator("a[href='/new-order']").click();

        myShopUiPage.locator("[aria-label=\"SKU\"]").fill(SKU + "-" + UUID.randomUUID().toString().substring(0, 8));
        myShopUiPage.locator("[aria-label=\"Quantity\"]").fill("invalid-quantity");
        myShopUiPage.locator("[aria-label=\"Place Order\"]").click();

        var errorAlert = myShopUiPage.locator("[role='alert'][data-notification-id]");
        errorAlert.waitFor();
        assertThat(errorAlert.isVisible()).isTrue();
        var errorText = errorAlert.textContent();
        assertThat(errorText)
                .contains("The request contains one or more validation errors")
                .contains("quantity")
                .contains("Quantity must be an integer");
    }
}
