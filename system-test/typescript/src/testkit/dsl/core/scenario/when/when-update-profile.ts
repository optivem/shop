import type { UpdateProfileRequest } from '../../../../driver/port/dtos/UpdateProfileRequest.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { DefaultGivens } from '../given/default-givens.js';
import { ThenProfileResultStage } from '../then/then-profile.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

/** Saves the profile as the logged-in user (the default customer unless chosen); unset fields keep their defaults. */
export class WhenUpdateProfile {
  private readonly request: UpdateProfileRequest = { ...DefaultGivens.profile() };

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withFirstName(firstName: string | null): this {
    this.request.firstName = firstName;
    return this;
  }

  withLastName(lastName: string | null): this {
    this.request.lastName = lastName;
    return this;
  }

  withStreet(street: string | null): this {
    this.request.street = street;
    return this;
  }

  withPostalCode(postalCode: string | null): this {
    this.request.postalCode = postalCode;
    return this;
  }

  withCity(city: string | null): this {
    this.request.city = city;
    return this;
  }

  withCountry(country: string | null): this {
    this.request.country = country;
    return this;
  }

  then(): ThenProfileResultStage;
  then(...args: unknown[]): ThenProfileResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenProfileResultStage(this.app, this.ctx, this.useCaseContext, { ...this.request });
  }
}
