package com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases;

import com.mycompany.myshop.backend.testkit.driver.port.external.erp.ErpDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.erp.usecases.base.BaseErpUseCase;

/**
 * The ERP liveness probe behind {@code assume().erp().shouldBeRunning()} — the one ERP use case that
 * asks a question instead of planting an answer. Named after the system-test project's {@code GoToErp}
 * so the same scenario line reads identically at both layers, though what it reaches differs: there a
 * deployed ERP container, here the in-process WireMock stub.
 *
 * <p>Unlike the system-test twin this returns no {@code Result} — component stub use cases are
 * {@code void execute()}, and an unreachable stub surfaces as a thrown exception rather than a
 * failed result.
 */
public class GoToErp extends BaseErpUseCase {

    public GoToErp(ErpDriver driver) {
        super(driver);
    }

    @Override
    public void execute() {
        driver.goToErp();
    }
}
