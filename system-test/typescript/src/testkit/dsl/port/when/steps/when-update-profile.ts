import type { ThenProfileResultStage } from '../../then/steps/then-profile.js';

export interface WhenUpdateProfile {
  withFirstName(firstName: string | null): this;
  withLastName(lastName: string | null): this;
  withStreet(street: string | null): this;
  withPostalCode(postalCode: string | null): this;
  withCity(city: string | null): this;
  withCountry(country: string | null): this;
  then(): ThenProfileResultStage;
}
