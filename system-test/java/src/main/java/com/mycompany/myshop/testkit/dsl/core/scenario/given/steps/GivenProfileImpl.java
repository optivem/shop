package com.mycompany.myshop.testkit.dsl.core.scenario.given.steps;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_CITY;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_FIRST_NAME;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_LAST_NAME;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_POSTAL_CODE;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_PROFILE_COUNTRY;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_STREET;

import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.dsl.core.scenario.given.GivenImpl;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.port.given.steps.GivenProfile;

public class GivenProfileImpl extends BaseGivenStep implements GivenProfile {
    private final GivenImpl given;
    private String firstName = DEFAULT_FIRST_NAME;
    private String lastName = DEFAULT_LAST_NAME;
    private String street = DEFAULT_STREET;
    private String postalCode = DEFAULT_POSTAL_CODE;
    private String city = DEFAULT_CITY;
    private String country = DEFAULT_PROFILE_COUNTRY;
    private String savedByAlias;

    public GivenProfileImpl(GivenImpl given) {
        super(given);
        this.given = given;
    }

    public GivenProfileImpl withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public GivenProfileImpl withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public GivenProfileImpl withStreet(String street) {
        this.street = street;
        return this;
    }

    public GivenProfileImpl withPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public GivenProfileImpl withCity(String city) {
        this.city = city;
        return this;
    }

    public GivenProfileImpl withCountry(String country) {
        this.country = country;
        return this;
    }

    public GivenProfileImpl savedByCustomer() {
        this.savedByAlias = null;
        return this;
    }

    public GivenProfileImpl savedByCustomer(String alias) {
        given.registerCustomer(alias);
        this.savedByAlias = alias;
        return this;
    }

    public boolean isSavedByDefaultCustomer() {
        return savedByAlias == null;
    }

    @Override
    public void execute(UseCaseDsl app) {
        app.actAs(savedByAlias == null ? given.defaultCustomer() : given.resolveCustomer(savedByAlias));
        try {
            app.myShop().updateProfile()
                    .firstName(firstName)
                    .lastName(lastName)
                    .street(street)
                    .postalCode(postalCode)
                    .city(city)
                    .country(country)
                    .execute()
                    .shouldSucceed();
        } finally {
            app.actAs(UserIdentity.DEFAULT);
        }
    }
}
