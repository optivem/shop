package com.mycompany.myshop.testkit.dsl.core.scenario.given.steps;

import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenErp;

/** The ERP's availability; an unavailable ERP fails every call, whatever products or promotion are staged. */
public class GivenErpImpl extends BaseGivenStep implements GivenErp {
    private boolean unavailable;

    public GivenErpImpl(GivenImpl given) {
        super(given);
    }

    @Override
    public GivenErpImpl unavailable() {
        this.unavailable = true;
        return this;
    }

    @Override
    public void execute(UseCaseDsl app) {
        if (unavailable) {
            app.erp().returnsUnavailable()
                    .execute()
                    .shouldSucceed();
        }
    }
}
