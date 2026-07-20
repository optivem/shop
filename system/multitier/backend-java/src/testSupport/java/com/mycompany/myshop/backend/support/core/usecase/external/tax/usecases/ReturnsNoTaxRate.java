package com.mycompany.myshop.backend.support.core.usecase.external.tax.usecases;

import com.mycompany.myshop.backend.support.harness.TaxStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.tax.usecases.base.BaseTaxUseCase;

public class ReturnsNoTaxRate extends BaseTaxUseCase {

    private String country;

    public ReturnsNoTaxRate(TaxStubDriver driver) {
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
