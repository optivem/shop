package com.mycompany.myshop.backend.testkit.driver.port.external.tax;

public interface TaxDriver {

    void goToTax();

    void returnsTaxRate(String country, String rate);

    void returnsNoTaxRate(String country);

    void failsForCountry(String country, int status, String body);
}
