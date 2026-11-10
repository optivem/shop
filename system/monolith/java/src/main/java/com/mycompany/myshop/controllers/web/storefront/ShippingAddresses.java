package com.mycompany.myshop.controllers.web.storefront;

import com.mycompany.myshop.core.dtos.ShippingAddress;
import com.mycompany.myshop.core.dtos.ViewProfileResponse;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Shipping address rules shared by checkout and the profile page (UC-009 BR-3, UC-010 BR-2/BR-3). */
public final class ShippingAddresses {

    public static final List<String> COUNTRIES = List.of("US", "DE", "GB");
    public static final String COUNTRY_NOT_SUPPORTED = "Country must be US, DE or GB";

    /**
     * Estimated sales tax per shipping country, for the checkout review only (the same rates as the tax
     * system's test data). Each order is taxed by the backend when it is placed.
     */
    public static final Map<String, BigDecimal> ESTIMATED_TAX_RATES = Map.of(
            "US", BigDecimal.ZERO, "DE", new BigDecimal("0.19"), "GB", new BigDecimal("0.20"));

    private ShippingAddresses() {
    }

    /** Field name -> error message; every field is required (spaces count as empty) and the country must be supported. */
    public static Map<String, String> validate(ShippingAddress address) {
        var errors = new LinkedHashMap<String, String>();
        require(errors, "firstName", address.getFirstName(), "First name");
        require(errors, "lastName", address.getLastName(), "Last name");
        require(errors, "street", address.getStreet(), "Street");
        require(errors, "postalCode", address.getPostalCode(), "Postal code");
        require(errors, "city", address.getCity(), "City");
        require(errors, "country", address.getCountry(), "Country");
        if (!errors.containsKey("country") && !COUNTRIES.contains(address.getCountry().trim())) {
            errors.put("country", COUNTRY_NOT_SUPPORTED);
        }
        return errors;
    }

    public static ShippingAddress addressOf(ViewProfileResponse profile) {
        return new ShippingAddress(orEmpty(profile.getFirstName()), orEmpty(profile.getLastName()), orEmpty(profile.getStreet()),
                orEmpty(profile.getPostalCode()), orEmpty(profile.getCity()), orEmpty(profile.getCountry()));
    }

    /** The profile's saved address, or null when the customer has not saved a complete one yet. */
    public static ShippingAddress savedAddressOf(ViewProfileResponse profile) {
        if (profile == null) {
            return null;
        }
        var address = addressOf(profile);
        return validate(address).isEmpty() ? address : null;
    }

    /** "Ana Novak, Main St 1, 10115 Berlin, DE". */
    public static String format(ShippingAddress address) {
        return address.getFirstName() + " " + address.getLastName() + ", " + address.getStreet() + ", "
                + address.getPostalCode() + " " + address.getCity() + ", " + address.getCountry();
    }

    /** 0.19 -> "19%"; "-" for a country without a known rate. */
    public static String estimatedTaxRate(String country) {
        var rate = country == null ? null : ESTIMATED_TAX_RATES.get(country.trim());
        return rate == null ? "-" : rate.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
    }

    public static ShippingAddress empty() {
        return new ShippingAddress("", "", "", "", "", "");
    }

    private static void require(Map<String, String> errors, String field, String value, String label) {
        if (value == null || value.isBlank()) {
            errors.put(field, label + " is required");
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
