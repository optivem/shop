package com.mycompany.myshop.systemtest.latest.acceptance.base;

import com.mycompany.myshop.testkit.dsl.port.given.GivenStage;

/**
 * UC-009's Background: the same ERP product list as UC-006's, and the countries' tax rates US 0.00, DE 0.19 and
 * GB 0.20. Checkout offers only those real country codes (no aliases), so their rates are the tax stub's own
 * mappings for US, DE and GB rather than staged per scenario.
 */
public abstract class BaseCheckOutTest extends BaseManageCartTest {
    protected static final Address ANA_NOVAK_BERLIN = new Address("Ana", "Novak", "Main St 1", "10115", "Berlin", "DE");
    protected static final Address JOHN_SMITH_NEW_YORK = new Address("John", "Smith", "1 Main St", "10001", "New York", "US");
    protected static final Address JANE_BROWN_LONDON = new Address("Jane", "Brown", "1 High St", "SW1A 1AA", "London", "GB");
    protected static final Address MARIE_DUPONT_PARIS = new Address("Marie", "Dupont", "1 Rue de Rivoli", "75001", "Paris", "FR");

    /** Given the catalog, the user is logged in as a customer whose saved shipping address is this one. */
    protected GivenStage givenTheCatalogAndSavedAddress(Address address) {
        return givenTheCatalog()
                .and().profile()
                    .withFirstName(address.firstName())
                    .withLastName(address.lastName())
                    .withStreet(address.street())
                    .withPostalCode(address.postalCode())
                    .withCity(address.city())
                    .withCountry(address.country())
                    .savedByCustomer()
                .and().loggedInAsCustomer();
    }

    /** A shipping address, written in the specs as "Ana Novak, Main St 1, 10115 Berlin, DE". */
    protected record Address(String firstName, String lastName, String street, String postalCode, String city,
                             String country) {
        public String formatted() {
            return firstName + " " + lastName + ", " + street + ", " + postalCode + " " + city + ", " + country;
        }

        /** This address with one field (as the specs name it, e.g. "postal code") left empty. */
        public Address withEmpty(String field) {
            return switch (field) {
                case "first name" -> new Address("", lastName, street, postalCode, city, country);
                case "last name" -> new Address(firstName, "", street, postalCode, city, country);
                case "street" -> new Address(firstName, lastName, "", postalCode, city, country);
                case "postal code" -> new Address(firstName, lastName, street, "", city, country);
                case "city" -> new Address(firstName, lastName, street, postalCode, "", country);
                case "country" -> new Address(firstName, lastName, street, postalCode, city, "");
                default -> throw new IllegalArgumentException("Unknown address field: " + field);
            };
        }
    }
}
