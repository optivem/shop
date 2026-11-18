import { expect } from '@playwright/test';
import type { SystemError } from '../../../../driver/port/dtos/errors/SystemError.js';
import type { BrowseCustomersResponse } from '../../../../driver/port/dtos/BrowseCustomersResponse.js';
import { UserIdentity } from '../../../../driver/port/user-identity.js';
import { BrowseCustomersVerification } from '../../usecase/usecases/BrowseCustomersVerification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { arrangeGiven } from '../given/arrange-given.js';

export class ThenBrowseCustomersResultStage implements PromiseLike<void> {
  private _executionPromise: Promise<void> | null = null;
  private _expectSuccess = true;
  private readonly _errorAssertions: ((error: SystemError, useCaseContext: UseCaseContext) => void)[] = [];
  private readonly _customersAssertions: ((customers: BrowseCustomersResponse, useCaseContext: UseCaseContext) => void)[] = [];

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  /** The username of the customer known in this scenario by the given alias; valid once the scenario is set up. */
  _customerUsername(alias: string): string {
    return UserIdentity.usernameOf(this.ctx.customers.resolve(alias));
  }

  shouldSucceed(): ThenBrowseCustomersSuccess {
    this._expectSuccess = true;
    return new ThenBrowseCustomersSuccess(this);
  }

  shouldFail(): ThenBrowseCustomersFailure {
    this._expectSuccess = false;
    return new ThenBrowseCustomersFailure(this);
  }

  _addErrorAssertion(fn: (error: SystemError, useCaseContext: UseCaseContext) => void): void {
    this._errorAssertions.push(fn);
  }

  _addCustomersAssertion(fn: (customers: BrowseCustomersResponse, useCaseContext: UseCaseContext) => void): void {
    this._customersAssertions.push(fn);
  }

  private async _execute(): Promise<void> {
    if (this._executionPromise) return this._executionPromise;
    this._executionPromise = this._doExecute();
    return this._executionPromise;
  }

  private async _doExecute(): Promise<void> {
    await arrangeGiven(this.app, this.ctx, this.useCaseContext);

    const result = await this.app.myShop('dynamic').browseCustomers({});
    if (this._expectSuccess) {
      expect(result.success, JSON.stringify(result)).toBe(true);
      if (result.success) {
        for (const fn of this._customersAssertions) fn(result.value, this.useCaseContext);
      }
    } else {
      expect(result.success, JSON.stringify(result)).toBe(false);
      if (!result.success) {
        for (const fn of this._errorAssertions) fn(result.error, this.useCaseContext);
      }
    }
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this._execute().then(onfulfilled, onrejected);
  }
}

export class ThenBrowseCustomersSuccess implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseCustomersResultStage) {}

  and(): this {
    return this;
  }

  /** The customer list the executed browse returned. */
  customerList(): ThenBrowseCustomersCustomerList {
    return new ThenBrowseCustomersCustomerList(this.stage);
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}

export class ThenBrowseCustomersCustomerList implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseCustomersResultStage) {}

  and(): this {
    return this;
  }

  /**
   * Each of these customers (aliases) is listed once, in this order. Every customer who has ever ordered on the
   * shared stack is listed too, so others may come before, between or after them.
   */
  listsCustomersInOrder(...customerAliases: string[]): this {
    this.stage._addCustomersAssertion((response, useCaseContext) => {
      const usernames = customerAliases.map((alias) => this.stage._customerUsername(alias));
      new BrowseCustomersVerification(response, useCaseContext).listsCustomersInOrder(usernames);
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

export class ThenBrowseCustomersFailure implements PromiseLike<void> {
  constructor(private readonly stage: ThenBrowseCustomersResultStage) {}

  errorMessage(expected: string): this {
    this.stage._addErrorAssertion((error, useCaseContext) => {
      expect(error.message).toBe(useCaseContext.expandAliases(expected));
    });
    return this;
  }

  noOrderPersisted(): this {
    this.stage._addErrorAssertion((error) => {
      expect(error, 'Expected the rejected operation to yield an error response (nothing persisted)').toBeDefined();
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

  and(): this {
    return this;
  }

  then<TResult1 = void, TResult2 = never>(
    onfulfilled?: ((value: void) => TResult1 | PromiseLike<TResult1>) | null,
    onrejected?: ((reason: unknown) => TResult2 | PromiseLike<TResult2>) | null,
  ): PromiseLike<TResult1 | TResult2> {
    return this.stage.then(onfulfilled, onrejected);
  }
}
