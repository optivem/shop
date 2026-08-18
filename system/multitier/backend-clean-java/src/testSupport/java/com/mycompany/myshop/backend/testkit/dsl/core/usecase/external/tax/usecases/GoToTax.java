package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.tax.TaxDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.usecases.base.BaseTaxUseCase;

public class GoToTax extends BaseTaxUseCase {

    public GoToTax(TaxDriver driver) {
        super(driver);
    }

    @Override
    public void execute() {
        driver.goToTax();
    }
}
