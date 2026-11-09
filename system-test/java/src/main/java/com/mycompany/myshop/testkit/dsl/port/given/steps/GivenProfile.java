package com.mycompany.myshop.testkit.dsl.port.given.steps;

import com.mycompany.myshop.testkit.dsl.port.given.steps.base.GivenStep;

/** A saved profile; unset fields keep the defaults (Ana, Novak, Main St 1, 10115, Berlin, DE). */
public interface GivenProfile extends GivenStep {
    GivenProfile withFirstName(String firstName);

    GivenProfile withLastName(String lastName);

    GivenProfile withStreet(String street);

    GivenProfile withPostalCode(String postalCode);

    GivenProfile withCity(String city);

    GivenProfile withCountry(String country);

    /** The profile is saved by the default customer (customer1). */
    GivenProfile savedByCustomer();

    /** The profile is saved by the customer known in this scenario by the given alias. */
    GivenProfile savedByCustomer(String alias);
}
