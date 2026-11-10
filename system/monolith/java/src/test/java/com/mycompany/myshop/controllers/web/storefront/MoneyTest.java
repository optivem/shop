package com.mycompany.myshop.controllers.web.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void formatsWithThousandsSeparatorsAndTwoDecimals() {
        assertThat(Money.format(new BigDecimal("1299.99"))).isEqualTo("$1,299.99");
        assertThat(Money.format(new BigDecimal("99.9"))).isEqualTo("$99.90");
    }

    @Test
    void lineTotalIsUnitPriceTimesQuantity() {
        assertThat(Money.format(Money.lineTotal(new BigDecimal("699.99"), 3))).isEqualTo("$2,099.97");
    }
}
