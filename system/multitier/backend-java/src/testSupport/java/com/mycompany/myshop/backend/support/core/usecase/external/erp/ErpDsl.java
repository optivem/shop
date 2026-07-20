package com.mycompany.myshop.backend.support.core.usecase.external.erp;

import com.mycompany.myshop.backend.support.harness.ErpStubDriver;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.FailsForProduct;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.FailsForPromotion;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.ReturnsNoProduct;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.ReturnsProduct;
import com.mycompany.myshop.backend.support.core.usecase.external.erp.usecases.ReturnsPromotion;

/**
 * The ERP, as the component test sees it: a WireMock server whose answers the test programs.
 *
 * <pre>{@code
 * app.erp().returnsProduct().sku("BOOK-123").unitPrice("10.00").execute();
 * app.erp().returnsNoProduct().sku("MISSING-1").execute();
 * app.erp().returnsPromotion().active(true).discount("0.9").execute();
 * app.erp().failsForProduct().sku("BAD-SKU").status(500).body("Internal Server Error").execute();
 * app.erp().failsForPromotion().status(503).body("Service Unavailable").execute();
 * }</pre>
 *
 * <p>Prices and discounts are passed as {@code String} so the stubbed JSON is byte-identical to the
 * raw WireMock the {@code legacy/} tests inline.
 */
public class ErpDsl {

    private final ErpStubDriver driver;

    public ErpDsl(ErpStubDriver driver) {
        this.driver = driver;
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
