package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp;

import com.mycompany.myshop.backend.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.FailsForProduct;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.FailsForPromotion;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.GoToErp;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.ReturnsNoProduct;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.ReturnsProduct;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.ReturnsPromotion;

public class ErpDsl {

    private final ErpDriver driver;

    public ErpDsl(ErpDriver driver) {
        this.driver = driver;
    }

    public GoToErp goToErp() {
        return new GoToErp(driver);
    }

    public ReturnsProduct returnsProduct() {
        return new ReturnsProduct(driver);
    }

    public ReturnsNoProduct returnsNoProduct() {
        return new ReturnsNoProduct(driver);
    }

    public ReturnsPromotion returnsPromotion() {
        return new ReturnsPromotion(driver);
    }

    public FailsForProduct failsForProduct() {
        return new FailsForProduct(driver);
    }

    public FailsForPromotion failsForPromotion() {
        return new FailsForPromotion(driver);
    }
}
