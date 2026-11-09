package com.mycompany.myshop.backend.core.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Field names and messages of the profile and place-order validation, as the API reports them. */
class RequestValidationTest {

    private static final jakarta.validation.ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void close() {
        FACTORY.close();
    }

    @Test
    void completeProfileIsValid() {
        assertThat(errors(profile("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE"))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void everyEmptyProfileFieldIsReported(String empty) {
        assertThat(errors(profile(empty, empty, empty, empty, empty, empty))).containsExactlyInAnyOrderEntriesOf(Map.of(
                "firstName", "First name is required",
                "lastName", "Last name is required",
                "street", "Street is required",
                "postalCode", "Postal code is required",
                "city", "City is required",
                "country", "Country is required"));
    }

    @Test
    void nullProfileFieldsAreReported() {
        assertThat(errors(new UpdateProfileRequest())).hasSize(6).containsEntry("country", "Country is required");
    }

    @ParameterizedTest
    @ValueSource(strings = {"FR", "de", "DEU", " DE"})
    void unsupportedProfileCountryIsReported(String country) {
        assertThat(errors(profile("Ana", "Novak", "Main St 1", "10115", "Berlin", country)))
                .containsExactlyEntriesOf(Map.of("country", "Country must be US, DE or GB"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"US", "DE", "GB"})
    void supportedProfileCountryIsValid(String country) {
        assertThat(errors(profile("Ana", "Novak", "Main St 1", "10115", "Berlin", country))).isEmpty();
    }

    @Test
    void quickOrderRequiresTheTopLevelCountry() {
        var request = order();
        request.setCountry("  ");

        assertThat(errors(request)).containsExactlyEntriesOf(Map.of("country", "Country must not be empty"));
    }

    @Test
    void orderWithShippingAddressDoesNotNeedTheTopLevelCountry() {
        var request = order();
        request.setShippingAddress(new ShippingAddress("Ana", "Novak", "Main St 1", "10115", "Berlin", "XX"));

        assertThat(errors(request)).isEmpty();
    }

    @Test
    void everyEmptyShippingAddressFieldIsReportedWithTheOtherFieldErrors() {
        var request = order();
        request.setSku("");
        request.setShippingAddress(new ShippingAddress("", " ", null, "", "   ", ""));

        assertThat(errors(request)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "sku", "SKU must not be empty",
                "shippingAddress.firstName", "First name is required",
                "shippingAddress.lastName", "Last name is required",
                "shippingAddress.street", "Street is required",
                "shippingAddress.postalCode", "Postal code is required",
                "shippingAddress.city", "City is required",
                "shippingAddress.country", "Country is required"));
    }

    private static PlaceOrderRequest order() {
        var request = new PlaceOrderRequest();
        request.setSku("BOOK-123");
        request.setQuantity(1);
        return request;
    }

    private static UpdateProfileRequest profile(String firstName, String lastName, String street,
                                                String postalCode, String city, String country) {
        var request = new UpdateProfileRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setStreet(street);
        request.setPostalCode(postalCode);
        request.setCity(city);
        request.setCountry(country);
        return request;
    }

    private static <T> Map<String, String> errors(T request) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(request);
        return violations.stream().collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(), ConstraintViolation::getMessage));
    }
}
