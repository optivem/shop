package com.mycompany.myshop.testkit.dsl.core.scenario.then.steps;

import com.mycompany.myshop.testkit.dsl.core.scenario.ExecutionResultContext;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.usecase.UseCaseDsl;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.ViewProfileVerification;
import com.mycompany.myshop.testkit.dsl.port.then.steps.ThenProfile;

/**
 * Verifies a profile: the one the executed operation returned (view or save), or, when it returned none
 * (e.g. after a failure), the acting user's profile as viewed now. {@link #profile()} from here always views
 * it again, so {@code .and().profile()} after a save checks what was stored.
 */
public class ThenProfileImpl<R, V extends ResponseVerification<R>>
        extends BaseThenStep<R, V> implements ThenProfile {
    private final ViewProfileVerification profileVerification;

    public ThenProfileImpl(UseCaseDsl app, ExecutionResultContext executionResult, V successVerification,
                           boolean useOperationResult) {
        super(app, executionResult, successVerification);
        if (useOperationResult && successVerification instanceof ViewProfileVerification viewProfileVerification) {
            this.profileVerification = viewProfileVerification;
        } else {
            this.profileVerification = app.myShop().viewProfile().execute().shouldSucceed();
        }
    }

    public ThenProfileImpl<R, V> hasFirstName(String expectedFirstName) {
        profileVerification.firstName(expectedFirstName);
        return this;
    }

    public ThenProfileImpl<R, V> hasLastName(String expectedLastName) {
        profileVerification.lastName(expectedLastName);
        return this;
    }

    public ThenProfileImpl<R, V> hasEmail(String expectedEmail) {
        profileVerification.email(expectedEmail);
        return this;
    }

    public ThenProfileImpl<R, V> hasStreet(String expectedStreet) {
        profileVerification.street(expectedStreet);
        return this;
    }

    public ThenProfileImpl<R, V> hasPostalCode(String expectedPostalCode) {
        profileVerification.postalCode(expectedPostalCode);
        return this;
    }

    public ThenProfileImpl<R, V> hasCity(String expectedCity) {
        profileVerification.city(expectedCity);
        return this;
    }

    public ThenProfileImpl<R, V> hasCountry(String expectedCountry) {
        profileVerification.country(expectedCountry);
        return this;
    }

    public ThenProfileImpl<R, V> doesNotHaveStreet(String street) {
        profileVerification.streetIsNot(street);
        return this;
    }

    @Override
    public ThenProfileImpl<R, V> profile() {
        return new ThenProfileImpl<>(app, executionResult, successVerification, false);
    }

    @Override
    public ThenProfileImpl<R, V> and() {
        return this;
    }
}
