package com.mycompany.myshop.testkit.dsl.port.given.steps;

import com.mycompany.myshop.testkit.dsl.port.given.steps.base.GivenStep;

/**
 * A customer of the scenario's own: a new sign-in account with no saved profile and no orders. It becomes the
 * scenario's customer, so {@code loggedInAsCustomer()}, {@code profile().savedByCustomer()} and
 * {@code order().placedByCustomer()} all mean this customer. Unset names keep the defaults (Ana Novak); the email
 * is unique unless set.
 */
public interface GivenNewCustomer extends GivenStep {
    /** The sign-in account's first name. */
    GivenNewCustomer withFirstName(String firstName);

    /** The sign-in account's last name. */
    GivenNewCustomer withLastName(String lastName);

    /** The sign-in account's email. */
    GivenNewCustomer withEmail(String email);
}
