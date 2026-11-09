package com.mycompany.myshop.testkit.dsl.core.scenario.when.steps;

import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_CITY;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_FIRST_NAME;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_LAST_NAME;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_POSTAL_CODE;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_PROFILE_COUNTRY;
import static com.mycompany.myshop.testkit.dsl.core.scenario.ScenarioDefaults.DEFAULT_STREET;

import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileResponse;
import com.mycompany.myshop.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResult;
import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultBuilder;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.ViewProfileVerification;
import com.mycompany.myshop.testkit.dsl.port.ChannelMode;
import com.mycompany.myshop.testkit.dsl.port.when.steps.WhenUpdateProfile;

public class WhenUpdateProfileImpl extends BaseWhenStep<ViewProfileResponse, ViewProfileVerification> implements WhenUpdateProfile {
    private String firstName = DEFAULT_FIRST_NAME;
    private String lastName = DEFAULT_LAST_NAME;
    private String street = DEFAULT_STREET;
    private String postalCode = DEFAULT_POSTAL_CODE;
    private String city = DEFAULT_CITY;
    private String country = DEFAULT_PROFILE_COUNTRY;

    public WhenUpdateProfileImpl(UseCaseDsl app, ScenarioDslImpl scenario) {
        super(app, scenario);
    }

    public WhenUpdateProfileImpl withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public WhenUpdateProfileImpl withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public WhenUpdateProfileImpl withStreet(String street) {
        this.street = street;
        return this;
    }

    public WhenUpdateProfileImpl withPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    public WhenUpdateProfileImpl withCity(String city) {
        this.city = city;
        return this;
    }

    public WhenUpdateProfileImpl withCountry(String country) {
        this.country = country;
        return this;
    }

    @Override
    protected ExecutionResult<ViewProfileResponse, ViewProfileVerification> execute(UseCaseDsl app) {
        var result = app.myShop(ChannelMode.DYNAMIC).updateProfile()
                .firstName(firstName)
                .lastName(lastName)
                .street(street)
                .postalCode(postalCode)
                .city(city)
                .country(country)
                .execute();
        return new ExecutionResultBuilder<>(result).build();
    }
}
