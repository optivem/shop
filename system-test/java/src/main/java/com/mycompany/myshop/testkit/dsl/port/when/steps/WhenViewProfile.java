package com.mycompany.myshop.testkit.dsl.port.when.steps;

import com.mycompany.myshop.testkit.dsl.port.when.steps.base.WhenStep;

/** Views the acting user's profile. */
public interface WhenViewProfile extends WhenStep {
    /** A visitor who is asked to sign in signs in as the customer, and is then shown the profile (UI only). */
    WhenViewProfile withSignInWhenAsked();
}
