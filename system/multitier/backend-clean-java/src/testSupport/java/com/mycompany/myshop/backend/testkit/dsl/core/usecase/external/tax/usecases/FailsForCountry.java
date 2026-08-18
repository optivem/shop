package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.tax.TaxDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases.base.BaseTaxUseCase;

public class FailsForCountry extends BaseTaxUseCase {

    private String country;
    private int status;
    private String body;

    public FailsForCountry(TaxDriver driver) {
        super(driver);
    }

    public FailsForCountry country(String country) {
        this.country = country;
        return this;
    }

    public FailsForCountry status(int status) {
        this.status = status;
        return this;
    }

    public FailsForCountry body(String body) {
        this.body = body;
        return this;
    }

    @Override
    public void execute() {
        driver.failsForCountry(country, status, body);
    }
}
