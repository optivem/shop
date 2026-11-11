import type { ScenarioDsl } from '../../../../src/testkit/dsl/port/scenario-dsl.js';
import type { GivenStage } from '../../../../src/testkit/dsl/port/given/given-stage.js';
import { givenTheCatalog } from './browse-catalog-background.js';

// UC-009's Background: the same ERP product list as UC-006's, and the countries' tax rates US 0.00, DE 0.19 and
// GB 0.20. Checkout offers only those real country codes (no aliases), so their rates are the tax stub's own
// mappings for US, DE and GB rather than staged per scenario.

/** A shipping address, written in the specs as "Ana Novak, Main St 1, 10115 Berlin, DE". */
export class Address {
    constructor(
        readonly firstName: string,
        readonly lastName: string,
        readonly street: string,
        readonly postalCode: string,
        readonly city: string,
        readonly country: string,
    ) {}

    formatted(): string {
        return `${this.firstName} ${this.lastName}, ${this.street}, ${this.postalCode} ${this.city}, ${this.country}`;
    }

    /** This address with one field (as the specs name it, e.g. "postal code") left empty. */
    withEmpty(field: string): Address {
        const { firstName, lastName, street, postalCode, city, country } = this;
        switch (field) {
            case 'first name':
                return new Address('', lastName, street, postalCode, city, country);
            case 'last name':
                return new Address(firstName, '', street, postalCode, city, country);
            case 'street':
                return new Address(firstName, lastName, '', postalCode, city, country);
            case 'postal code':
                return new Address(firstName, lastName, street, '', city, country);
            case 'city':
                return new Address(firstName, lastName, street, postalCode, '', country);
            case 'country':
                return new Address(firstName, lastName, street, postalCode, city, '');
            default:
                throw new Error(`Unknown address field: ${field}`);
        }
    }
}

export const ANA_NOVAK_BERLIN = new Address('Ana', 'Novak', 'Main St 1', '10115', 'Berlin', 'DE');
export const JOHN_SMITH_NEW_YORK = new Address('John', 'Smith', '1 Main St', '10001', 'New York', 'US');
export const JANE_BROWN_LONDON = new Address('Jane', 'Brown', '1 High St', 'SW1A 1AA', 'London', 'GB');
export const MARIE_DUPONT_PARIS = new Address('Marie', 'Dupont', '1 Rue de Rivoli', '75001', 'Paris', 'FR');

/** Given the catalog, the user is logged in as a customer whose saved shipping address is this one. */
export function givenTheCatalogAndSavedAddress(scenario: ScenarioDsl, address: Address): GivenStage {
    return givenTheCatalog(scenario)
        .and()
        .profile()
        .withFirstName(address.firstName)
        .withLastName(address.lastName)
        .withStreet(address.street)
        .withPostalCode(address.postalCode)
        .withCity(address.city)
        .withCountry(address.country)
        .savedByCustomer()
        .and()
        .loggedInAsCustomer();
}
