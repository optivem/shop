import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenProfile {
  withFirstName(firstName: string): GivenProfile;
  withLastName(lastName: string): GivenProfile;
  withStreet(street: string): GivenProfile;
  withPostalCode(postalCode: string): GivenProfile;
  withCity(city: string): GivenProfile;
  withCountry(country: string): GivenProfile;
  /** The profile is saved by the customer known in this scenario by the given alias, or by the default customer (customer1) when no alias is given. */
  savedByCustomer(alias?: string): GivenProfile;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
