package com.mycompany.myshop.testkit.dsl.core.scenario.given.steps;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_FIRST_NAME;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_LAST_NAME;

import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenNewCustomer;

public class GivenNewCustomerImpl extends BaseGivenStep implements GivenNewCustomer {
    private String firstName = DEFAULT_FIRST_NAME;
    private String lastName = DEFAULT_LAST_NAME;
    private String email;
    private UserIdentity identity;

    public GivenNewCustomerImpl(GivenImpl given) {
        super(given);
    }

    @Override
    public GivenNewCustomerImpl withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    @Override
    public GivenNewCustomerImpl withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    @Override
    public GivenNewCustomerImpl withEmail(String email) {
        this.email = email;
        return this;
    }

    /** Creates the account, once. */
    @Override
    public void execute(UseCaseDsl app) {
        if (identity == null) {
            identity = app.newCustomer(firstName, lastName, email);
        }
    }

    /** The created customer; only valid once the scenario is being set up. */
    public UserIdentity identity() {
        if (identity == null) {
            throw new IllegalStateException("The new customer is created when the scenario is set up");
        }
        return identity;
    }
}
