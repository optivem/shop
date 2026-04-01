package com.optivem.eshop.systemtest.legacy.mod06.e2e;

import com.optivem.eshop.dsl.channel.ChannelType;
import com.optivem.eshop.systemtest.legacy.mod06.e2e.base.BaseE2eTest;
import com.optivem.testing.Channel;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.optivem.eshop.dsl.common.ResultAssert.assertThatResult;
import static org.assertj.core.api.Assertions.assertThat;

class ViewOrderNegativeTest extends BaseE2eTest {
    private static Stream<Arguments> provideNonExistentOrderValues() {
        return Stream.of(
                Arguments.of("NON-EXISTENT-ORDER-99999", "Order NON-EXISTENT-ORDER-99999 does not exist."),
                Arguments.of("NON-EXISTENT-ORDER-88888", "Order NON-EXISTENT-ORDER-88888 does not exist."),
                Arguments.of("NON-EXISTENT-ORDER-77777", "Order NON-EXISTENT-ORDER-77777 does not exist.")
        );
    }

    @TestTemplate
    @Channel({ChannelType.UI, ChannelType.API})
    @MethodSource("provideNonExistentOrderValues")
    void shouldNotBeAbleToViewNonExistentOrder(String orderNumber, String expectedErrorMessage) {
        var result = shopDriver.viewOrder(orderNumber);

        assertThatResult(result).isFailure();
        var error = result.getError();
        assertThat(error.getMessage()).isEqualTo(expectedErrorMessage);
    }
}

