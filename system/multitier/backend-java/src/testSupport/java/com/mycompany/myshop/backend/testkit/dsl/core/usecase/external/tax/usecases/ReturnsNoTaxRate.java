package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.tax.TaxDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases.base.BaseTaxUseCase;

public class ReturnsNoTaxRate extends BaseTaxUseCase {

    private String country;

    public ReturnsNoTaxRate(TaxDriver driver) {
        super(driver);
    }

    public ReturnsNoTaxRate country(String country) {
        this.country = country;
        return this;
    }

    @Override
    public void execute() {
        driver.stubTaxMissing(country);
    }
}
