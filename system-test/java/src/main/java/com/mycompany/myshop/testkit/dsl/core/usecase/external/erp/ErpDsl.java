package com.mycompany.myshop.testkit.dsl.core.usecase.external.erp;

import com.mycompany.myshop.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.GetProduct;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.GetProducts;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.GoToErp;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.ReturnsProduct;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.ReturnsProducts;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.ReturnsPromotion;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.ReturnsAvailable;
import com.mycompany.myshop.testkit.dsl.core.usecase.external.erp.usecases.ReturnsUnavailable;
import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

public class ErpDsl implements AutoCloseable {
    protected final ErpDriver driver;
    protected final UseCaseContext context;

    public ErpDsl(ErpDriver driver, UseCaseContext context) {
        this.driver = driver;
        this.context = context;
    }

    @Override
    public void close() {
        Closer.close(driver);
    }

    public GoToErp goToErp() {
        return new GoToErp(driver, context);
    }

    public ReturnsProduct returnsProduct() {
        return new ReturnsProduct(driver, context);
    }

    public GetProduct getProduct() {
        return new GetProduct(driver, context);
    }

    public ReturnsProducts returnsProducts() {
        return new ReturnsProducts(driver, context);
    }

    public GetProducts getProducts() {
        return new GetProducts(driver, context);
    }

    public ReturnsPromotion returnsPromotion() {
        return new ReturnsPromotion(driver, context);
    }

    public ReturnsUnavailable returnsUnavailable() {
        return new ReturnsUnavailable(driver, context);
    }

    public ReturnsAvailable returnsAvailable() {
        return new ReturnsAvailable(driver, context);
    }
}
