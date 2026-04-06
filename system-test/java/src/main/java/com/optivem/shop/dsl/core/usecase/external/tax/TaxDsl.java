package com.optivem.shop.dsl.core.usecase.external.tax;

import com.optivem.shop.dsl.common.Closer;
import com.optivem.shop.dsl.core.shared.UseCaseContext;
import com.optivem.shop.dsl.core.usecase.external.tax.usecases.GetTaxRate;
import com.optivem.shop.dsl.core.usecase.external.tax.usecases.GoToTax;
import com.optivem.shop.dsl.core.usecase.external.tax.usecases.ReturnsTaxRate;
import com.optivem.shop.dsl.driver.port.external.tax.TaxDriver;

public class TaxDsl implements AutoCloseable {
    private final TaxDriver driver;
    private final UseCaseContext context;

    public TaxDsl(TaxDriver driver, UseCaseContext context) {
        this.driver = driver;
        this.context = context;
    }

    @Override
    public void close() {
        Closer.close(driver);
    }

    public GoToTax goToTax() {
        return new GoToTax(driver, context);
    }

    public ReturnsTaxRate returnsTaxRate() {
        return new ReturnsTaxRate(driver, context);
    }

    public GetTaxRate getTaxRate() {
        return new GetTaxRate(driver, context);
    }
}
