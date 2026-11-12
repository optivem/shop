import { expect } from '@playwright/test';
import type { SystemError } from '../../../../driver/port/dtos/errors/SystemError.js';
import type { UpdateProfileRequest } from '../../../../driver/port/dtos/UpdateProfileRequest.js';
import type { ViewProfileRequest } from '../../../../driver/port/dtos/ViewProfileRequest.js';
import type { ViewProfileResponse } from '../../../../driver/port/dtos/ViewProfileResponse.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import { arrangeGiven } from '../given/arrange-given.js';
import type { ScenarioContext } from '../scenario-context.js';

type ProfileAssertion = (profile: ViewProfileResponse) => void;

/**
 * Runs viewProfile (no request) or updateProfile (with one) as the logged-in user. Profile assertions check the
 * operation's response and, after an update, also the profile as read back; after a failure they check the profile
 * as read back, so a test can show a rejected change left the saved profile as it was. What only the operation's own
 * response carries (the UI's message after saving, its email lock, country choices and tax note) is checked on that
 * response only.
 */
export class ThenProfileResultStage implements PromiseLike<void> {
  private _expectSuccess = true;
  private readonly _profileAssertions: ProfileAssertion[] = [];
  private readonly _responseAssertions: ProfileAssertion[] = [];
  private readonly _errorAssertions: ((error: SystemError, useCaseContext: UseCaseContext) => void)[] = [];
  private _executionPromise: Promise<void> | null = null;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
    private readonly updateRequest: UpdateProfileRequest | null,
    private readonly viewRequest: ViewProfileRequest = {},
  ) {}

  shouldSucceed(): ThenProfileSuccess {
    this._expectSuccess = true;
    return new ThenProfileSuccess(this);
  }

  shouldFail(): ThenProfileFailure {
    this._expectSuccess = false;
    return new ThenProfileFailure(this);
  }

  _addProfileAssertion(fn: ProfileAssertion): void {
    this._profileAssertions.push(fn);
  }

  _addResponseAssertion(fn: ProfileAssertion): void {
    this._responseAssertions.push(fn);
  }

  _addErrorAssertion(fn: (error: SystemError, useCaseContext: UseCaseContext) => void): void {
    this._errorAssertions.push(fn);
  }

  private async execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    await arrangeGiven(this.app, this.ctx, this.useCaseContext, { declaredOnly: true });

    const shop = this.app.myShop('dynamic');
    const result = this.updateRequest ? await shop.updateProfile(this.updateRequest) : await shop.viewProfile(this.viewRequest);

    if (this._expectSuccess) {
      expect(result.success, JSON.stringify(result)).toBe(true);
      if (!result.success) return;
      this.runProfileAssertions(result.value);
      for (const fn of this._responseAssertions) fn(result.value);
      if (this.updateRequest) await this.runProfileAssertionsOnSavedProfile();
    } else {
      expect(result.success, JSON.stringify(result)).toBe(false);
      if (result.success) return;
      for (const fn of this._errorAssertions) fn(result.error, this.useCaseContext);
      await this.runProfileAssertionsOnSavedProfile();
    }
  }

  private runProfileAssertions(profile: ViewProfileResponse): void {
    for (const fn of this._profileAssertions) fn(profile);
  }

  private async runProfileAssertionsOnSavedProfile(): Promise<void> {
    if (this._profileAssertions.length === 0) return;
    const saved = await this.app.myShop('dynamic').viewProfile({});
    expect(saved.success, JSON.stringify(saved)).toBe(true);
    if (saved.success) this.runProfileAssertions(saved.value);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.execute().then(onfulfilled, onrejected);
  }
}

export class ThenProfileSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenProfileResultStage) {}

  and(): this {
    return this;
  }

  profile(): ThenProfile {
    return new ThenProfile(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenProfileFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenProfileResultStage) {}

  and(): this {
    return this;
  }

  errorMessage(expected: string): this {
    this.stage._addErrorAssertion((error, useCaseContext) => {
      expect(error.message).toBe(useCaseContext.expandAliases(expected));
    });
    return this;
  }

  fieldErrorMessage(field: string, message: string): this {
    this.stage._addErrorAssertion((error, useCaseContext) => {
      const expandedMessage = useCaseContext.expandAliases(message);
      const fieldError = error.fieldErrors.find((fe) => fe.field === field);
      expect(fieldError?.message, `Expected field error for '${field}' in ${JSON.stringify(error.fieldErrors)}`).toBe(expandedMessage);
    });
    return this;
  }

  /** The profile as saved, read back as the acting user. */
  profile(): ThenProfile {
    return new ThenProfile(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

/** Profile values are literals (the profile accepts only US, DE or GB), so nothing is alias-resolved. */
export class ThenProfile implements PromiseLike<void> {
  constructor(private readonly stage: ThenProfileResultStage) {}

  and(): this {
    return this;
  }

  hasFirstName(expected: string): this {
    return this.has('firstName', expected);
  }

  hasLastName(expected: string): this {
    return this.has('lastName', expected);
  }

  hasEmail(expected: string): this {
    return this.has('email', expected);
  }

  hasStreet(expected: string): this {
    return this.has('street', expected);
  }

  hasPostalCode(expected: string): this {
    return this.has('postalCode', expected);
  }

  hasCity(expected: string): this {
    return this.has('city', expected);
  }

  hasCountry(expected: string): this {
    return this.has('country', expected);
  }

  doesNotHaveStreet(unexpected: string): this {
    this.stage._addProfileAssertion((profile) => {
      expect(profile.street, 'Profile street').not.toBe(unexpected);
    });
    return this;
  }

  /** No shipping address is saved: the street, postal code, city and country are empty (null or ""). */
  hasNoSavedAddress(): this {
    this.stage._addProfileAssertion((profile) => {
      const address = [profile.street, profile.postalCode, profile.city, profile.country];
      expect(
        address.every((value) => value === null || value === ''),
        `Expected no saved address, but the profile has ${JSON.stringify(address)}`,
      ).toBe(true);
    });
    return this;
  }

  /** The email is shown but cannot be edited: it comes from the sign-in account (UI only). */
  hasEmailThatCannotBeChanged(): this {
    this.stage._addResponseAssertion((profile) => {
      expect(profile.emailChangeable, 'Profile email changeable').toBe(false);
    });
    return this;
  }

  /** The country select offers exactly these countries, in this order (UI only). */
  hasCountryChoices(...expected: string[]): this {
    this.stage._addResponseAssertion((profile) => {
      expect(profile.countryChoices, 'Profile country choices').toEqual(expected);
    });
    return this;
  }

  /** The profile's note on how sales tax is charged (UI only). */
  hasTaxNotice(expected: string): this {
    this.stage._addResponseAssertion((profile) => {
      expect(profile.taxNotice, 'Profile tax notice').toBe(expected);
    });
    return this;
  }

  /** The customer was told this after saving (UI only). */
  hasMessage(expected: string): this {
    this.stage._addResponseAssertion((profile) => {
      expect(profile.message, 'Message shown after saving the profile').toBe(expected);
    });
    return this;
  }

  private has(field: keyof ViewProfileResponse, expected: string): this {
    this.stage._addProfileAssertion((profile) => {
      expect(profile[field], `Profile ${field}`).toBe(expected);
    });
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}
