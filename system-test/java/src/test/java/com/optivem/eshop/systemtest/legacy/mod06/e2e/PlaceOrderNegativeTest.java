package com.optivem.eshop.systemtest.legacy.mod06.e2e;

import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.dsl.driver.port.shop.dtos.PlaceOrderRequest;
import com.optivem.eshop.systemtest.legacy.mod06.e2e.base.BaseE2eTest;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;

import java.util.UUID;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static com.optivem.eshop.systemtest.commons.constants.Defaults.SKU;
import static org.assertj.core.api.Assertions.assertThat;

class PlaceOrderNegativeTest extends BaseE2eTest {
    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    void shouldRejectOrderWithNonIntegerQuantity() {
        var request = PlaceOrderRequest.builder()
                .sku(SKU + "-" + UUID.randomUUID().toString().substring(0, 8))
                .quantity("3.5")
                .build();

        var result = shopDriver.placeOrder(request);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo("The request contains one or more validation errors");
        assertThat(error.getFields()).anySatisfy(field -> {
            assertThat(field.getField()).isEqualTo("quantity");
            assertThat(field.getMessage()).isEqualTo("Quantity must be an integer");
        });
    }
}
