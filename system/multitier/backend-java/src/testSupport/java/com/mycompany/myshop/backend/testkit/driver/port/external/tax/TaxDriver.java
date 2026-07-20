package com.mycompany.myshop.backend.testkit.driver.port.external.tax;

/**
 * What the use case DSL needs from the Tax system: the ability to program its responses. Sibling of
 * {@code ErpDriver} — see that port for why the vocabulary is stub-programming only.
 */
public interface TaxDriver {

    /** Liveness probe behind {@code assume().tax().shouldBeRunning()}. See {@code ErpDriver}. */
    void goToTax();

    void stubTax(String country, String rate);

    void stubTaxMissing(String country);

    void stubTaxError(String country, int status, String body);
}
