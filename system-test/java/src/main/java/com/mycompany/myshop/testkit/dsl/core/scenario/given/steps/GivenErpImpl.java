package com.mycompany.myshop.testkit.dsl.core.scenario.given.steps;

import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenErp;

/**
 * The ERP's availability; an unavailable ERP fails every call, and a slow ERP answers every call only after
 * the system's outbound timeout, whatever products or promotion are staged.
 */
public class GivenErpImpl extends BaseGivenStep implements GivenErp {
    private boolean unavailable;
    private boolean slow;

    public GivenErpImpl(GivenImpl given) {
        super(given);
    }

    @Override
    public GivenErpImpl unavailable() {
        this.unavailable = true;
        return this;
    }

    @Override
    public GivenErpImpl slow() {
        this.slow = true;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        if (slow) {
            app.erp().returnsSlow()
                    .execute()
                    .shouldSucceed();
        }
        if (unavailable) {
            app.erp().returnsUnavailable()
                    .execute()
                    .shouldSucceed();
        }
    }
}
