package com.mycompany.myshop.backend.presentation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mycompany.myshop.backend.usecases.queries.order.ViewOrderDetailsResponse;
import com.mycompany.myshop.backend.usecases.queries.report.ViewSalesReportTopSkuResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyJsonModuleTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new MoneyJsonModule());

    @Test
    void writesAmountsAsTwoDecimalStringsAndRatesAsFourDecimalStrings() throws Exception {
        var response = new ViewOrderDetailsResponse(
                "ORD-1", Instant.parse("2026-01-01T00:00:00Z"), "SKU-1", 2,
                new BigDecimal("20"), new BigDecimal("40.0"), new BigDecimal("0.2"), new BigDecimal("8.005"),
                new BigDecimal("32.00"), new BigDecimal("0.07"), new BigDecimal("2.24"), new BigDecimal("22"),
                "PLACED", "US", null);

        var json = objectMapper.writeValueAsString(response);

        assertThat(json)
                .contains("\"unitPrice\":\"20.00\"")
                .contains("\"basePrice\":\"40.00\"")
                .contains("\"discountRate\":\"0.2000\"")
                .contains("\"discountAmount\":\"8.01\"")
                .contains("\"subtotalPrice\":\"32.00\"")
                .contains("\"taxRate\":\"0.0700\"")
                .contains("\"taxAmount\":\"2.24\"")
                .contains("\"totalPrice\":\"22.00\"")
                .contains("\"quantity\":2");
    }

    @Test
    void writesReportMoneyAsTwoDecimalStrings() throws Exception {
        var json = objectMapper.writeValueAsString(new ViewSalesReportTopSkuResponse("SKU-1", 3, 5, new BigDecimal("150")));

        assertThat(json).contains("\"totalPrice\":\"150.00\"").contains("\"orderCount\":3");
    }
}
