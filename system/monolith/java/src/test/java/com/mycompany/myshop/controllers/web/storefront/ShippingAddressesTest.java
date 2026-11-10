package com.mycompany.myshop.controllers.web.storefront;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.mycompany.myshop.core.dtos.ShippingAddress;
import org.junit.jupiter.api.Test;

class ShippingAddressesTest {

    @Test
    void aCompleteSupportedAddressIsValid() {
        assertThat(ShippingAddresses.validate(new ShippingAddress("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE"))).isEmpty();
    }

    @Test
    void everyFieldIsRequiredAndSpacesCountAsEmpty() {
        var errors = ShippingAddresses.validate(new ShippingAddress("Ana", "Novak", "Main St 1", "10115", "   ", ""));

        assertThat(errors).containsEntry("city", "City is required").containsEntry("country", "Country is required").hasSize(2);
    }

    @Test
    void theCountryMustBeAShippingCountry() {
        var errors = ShippingAddresses.validate(new ShippingAddress("Marie", "Dupont", "1 Rue de Rivoli", "75001", "Paris", "FR"));

        assertThat(errors).containsExactlyEntriesOf(Map.of("country", "Country must be US, DE or GB"));
    }

    @Test
    void formatsTheAddressOnOneLine() {
        assertThat(ShippingAddresses.format(new ShippingAddress("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE")))
                .isEqualTo("Ana Novak, Main St 1, 10115 Berlin, DE");
    }

    @Test
    void estimatesTheTaxRateOfTheShippingCountry() {
        assertThat(ShippingAddresses.estimatedTaxRate("DE")).isEqualTo("19%");
        assertThat(ShippingAddresses.estimatedTaxRate("GB")).isEqualTo("20%");
        assertThat(ShippingAddresses.estimatedTaxRate("US")).isEqualTo("0%");
        assertThat(ShippingAddresses.estimatedTaxRate("FR")).isEqualTo("-");
    }
}
