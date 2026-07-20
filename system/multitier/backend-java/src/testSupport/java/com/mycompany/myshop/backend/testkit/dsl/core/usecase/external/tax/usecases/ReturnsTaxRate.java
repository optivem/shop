package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.tax.TaxDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases.base.BaseTaxUseCase;

public class ReturnsTaxRate extends BaseTaxUseCase {

    private String country;
    private String taxRate;

    public ReturnsTaxRate(TaxDriver driver) {
        super(driver);
    }

    public ReturnsTaxRate country(String country) {
        this.country = country;
        return this;
    }

    public ReturnsTaxRate taxRate(String taxRate) {
        this.taxRate = taxRate;
        return this;
    }

    @Override
    public void execute() {
        driver.stubTax(country, taxRate);
    }
}
