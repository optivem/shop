package com.mycompany.myshop.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.core.dtos.ViewOrderDetailsResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyJsonModuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new MoneyJsonModule());

    @Test
    void writesAmountsAsTwoDecimalStringsAndRatesAsFourDecimalStrings() throws Exception {
        var response = new ViewOrderDetailsResponse();
        response.setUnitPrice(new BigDecimal("20"));
        response.setBasePrice(new BigDecimal("40.0"));
        response.setDiscountRate(new BigDecimal("0.2"));
        response.setDiscountAmount(new BigDecimal("8.005"));
        response.setSubtotalPrice(new BigDecimal("32.00"));
        response.setTaxRate(new BigDecimal("0.07"));
        response.setTaxAmount(new BigDecimal("2.24"));
        response.setTotalPrice(new BigDecimal("22"));

        var json = objectMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"unitPrice\":\"20.00\"")
                .contains("\"basePrice\":\"40.00\"")
                .contains("\"discountRate\":\"0.2000\"")
                .contains("\"discountAmount\":\"8.01\"")
                .contains("\"subtotalPrice\":\"32.00\"")
                .contains("\"taxRate\":\"0.0700\"")
                .contains("\"taxAmount\":\"2.24\"")
                .contains("\"totalPrice\":\"22.00\"");
    }

    @Test
    void writesOtherDecimalsAsPlainStrings() throws Exception {
        var json = objectMapper.writeValueAsString(Map.of("weight", new BigDecimal("1E+1")));

        assertThat(json).isEqualTo("{\"weight\":\"10\"}");
    }
}
