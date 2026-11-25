import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import type { SignUpRequest } from '../../../../driver/port/dtos/SignUpRequest.js';
import type { SignUpResponse } from '../../../../driver/port/dtos/SignUpResponse.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import { VoidVerification } from '../../shared/void-verification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';

/** A visitor signs up (UC-013) with this account and shipping country (UI only). */
export class SignUp extends BaseMyShopUseCase<SignUpResponse, VoidVerification> {
  private readonly request: SignUpRequest = {
    username: '',
    password: '',
    email: '',
    firstName: '',
    lastName: '',
    country: '',
  };

  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  account(username: string, password: string): this {
    this.request.username = username;
    this.request.password = password;
    return this;
  }

  email(email: string): this {
    this.request.email = email;
    return this;
  }

  name(firstName: string, lastName: string): this {
    this.request.firstName = firstName;
    this.request.lastName = lastName;
    return this;
  }

  country(country: string): this {
    this.request.country = country;
    return this;
  }

  async execute(): Promise<UseCaseResult<SignUpResponse, VoidVerification>> {
    const result = await this.driver.signUp({ ...this.request });

    return new UseCaseResult(
      result,
      this.context,
      (_, ctx) => new VoidVerification(undefined, ctx),
    );
  }
}
