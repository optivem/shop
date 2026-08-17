package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp;

import com.mycompany.myshop.backend.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.FailsForProduct;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.FailsForPromotion;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.GoToErp;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.ReturnsNoProduct;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.ReturnsProduct;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.ReturnsPromotion;

/**
 * The ERP, as arranged through whichever {@link ErpDriver} is supplied — {@code ErpStubDriver} for the
 * {@code component} suite (permanently stub-only there), {@code ErpRealDriver} for the real-mode parity
 * contract tests.
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
