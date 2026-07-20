package com.mycompany.myshop.backend.support.core.usecase.external.tax;

import com.mycompany.myshop.backend.support.harness.TaxStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.tax.usecases.FailsForCountry;
import com.mycompany.myshop.backend.support.core.usecase.external.tax.usecases.ReturnsNoTaxRate;
import com.mycompany.myshop.backend.support.core.usecase.external.tax.usecases.ReturnsTaxRate;

/**
 * The Tax system, as the component test sees it.
 *
 * <pre>{@code
 * app.tax().returnsTaxRate().country("US").taxRate("0.10").execute();
 * app.tax().returnsNoTaxRate().country("ZZ").execute();
 * app.tax().failsForCountry().country("US").status(500).body("Internal Server Error").execute();
 * }</pre>
 *
 * <p>Rates are passed as {@code String} so the stubbed JSON is byte-identical to the raw WireMock
 * the {@code legacy/} tests inline.
 */
public class TaxDsl {

    private final TaxStubDriver driver;

    public TaxDsl(TaxStubDriver driver) {
        this.driver = driver;
    }

    public ReturnsTaxRate returnsTaxRate() {
        return new ReturnsTaxRate(driver);
    }

    public ReturnsNoTaxRate returnsNoTaxRate() {
        return new ReturnsNoTaxRate(driver);
    }

    public FailsForCountry failsForCountry() {
        return new FailsForCountry(driver);
    }
}
