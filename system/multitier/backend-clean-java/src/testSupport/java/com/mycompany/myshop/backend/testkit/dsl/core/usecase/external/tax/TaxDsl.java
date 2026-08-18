package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax;

import com.mycompany.myshop.backend.testkit.driver.port.external.tax.TaxDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases.FailsForCountry;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases.GoToTax;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases.ReturnsNoTaxRate;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases.ReturnsTaxRate;

public class TaxDsl {

    private final TaxDriver driver;

    public TaxDsl(TaxDriver driver) {
        this.driver = driver;
    }

    public GoToTax goToTax() {
        return new GoToTax(driver);
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
