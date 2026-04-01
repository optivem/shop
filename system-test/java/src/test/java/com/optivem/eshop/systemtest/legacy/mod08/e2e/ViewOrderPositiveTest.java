package com.optivem.eshop.systemtest.legacy.mod08.e2e;

import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.eshop.systemtest.legacy.mod08.e2e.base.BaseE2eTest;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

import static com.optivem.eshop.systemtest.commons.constants.Defaults.*;

class ViewOrderPositiveTest extends BaseE2eTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldViewPlacedOrder() {
        scenario
                .given().product().withSku(SKU).withUnitPrice(25.00)
                .and().order().withOrderNumber(ORDER_NUMBER).withSku(SKU).withCountry(COUNTRY).withQuantity(4)
                .when().viewOrder().withOrderNumber(ORDER_NUMBER)
                .then().shouldSucceed()
                .and().order(ORDER_NUMBER)
                .hasSku(SKU)
                .hasQuantity(4)
                .hasCountry(COUNTRY)
                .hasUnitPrice(25.00)
                .hasSubtotalPrice(100.00)
                .hasStatus(OrderStatus.PLACED)
                .hasDiscountRateGreaterThanOrEqualToZero()
                .hasDiscountAmountGreaterThanOrEqualToZero()
                .hasSubtotalPriceGreaterThanZero()
                .hasTaxRateGreaterThanOrEqualToZero()
                .hasTaxAmountGreaterThanOrEqualToZero()
                .hasTotalPriceGreaterThanZero();
    }
}



