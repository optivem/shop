import type { MyShopDriver } from '../../../../driver/port/my-shop-driver.js';
import type { UpdateProfileRequest } from '../../../../driver/port/dtos/UpdateProfileRequest.js';
import type { ViewProfileResponse } from '../../../../driver/port/dtos/ViewProfileResponse.js';
import { UseCaseResult } from '../../shared/use-case-result.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { BaseMyShopUseCase } from './base/BaseMyShopUseCase.js';
import { ViewProfileVerification } from './ViewProfileVerification.js';

/** Profile values are literals (the profile accepts only US, DE or GB), so nothing is alias-resolved. */
export class UpdateProfile extends BaseMyShopUseCase<ViewProfileResponse, ViewProfileVerification> {
  private readonly request: UpdateProfileRequest = {
    firstName: null,
    lastName: null,
    street: null,
    postalCode: null,
    city: null,
    country: null,
  };

  constructor(driver: MyShopDriver, context: UseCaseContext) {
    super(driver, context);
  }

  firstName(firstName: string | null): this {
    this.request.firstName = firstName;
    return this;
  }

  lastName(lastName: string | null): this {
    this.request.lastName = lastName;
    return this;
  }

  street(street: string | null): this {
    this.request.street = street;
    return this;
  }

  postalCode(postalCode: string | null): this {
    this.request.postalCode = postalCode;
    return this;
  }

  city(city: string | null): this {
    this.request.city = city;
    return this;
  }

  country(country: string | null): this {
    this.request.country = country;
    return this;
  }

  async execute(): Promise<UseCaseResult<ViewProfileResponse, ViewProfileVerification>> {
    const result = await this.driver.updateProfile({ ...this.request });

    return new UseCaseResult(
      result,
      this.context,
      (response, ctx) => new ViewProfileVerification(response, ctx),
    );
  }
}
