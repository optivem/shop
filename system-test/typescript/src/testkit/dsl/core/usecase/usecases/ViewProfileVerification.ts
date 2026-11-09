import { expect } from '@playwright/test';
import type { ViewProfileResponse } from '../../../../driver/port/dtos/ViewProfileResponse.js';
import { ResponseVerification } from '../../shared/response-verification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';

export class ViewProfileVerification extends ResponseVerification<ViewProfileResponse> {
  constructor(response: ViewProfileResponse, context: UseCaseContext) {
    super(response, context);
  }

  firstName(expected: string | null): this {
    expect(this.getResponse().firstName, 'Profile firstName').toBe(expected);
    return this;
  }

  lastName(expected: string | null): this {
    expect(this.getResponse().lastName, 'Profile lastName').toBe(expected);
    return this;
  }

  email(expected: string | null): this {
    expect(this.getResponse().email, 'Profile email').toBe(expected);
    return this;
  }

  street(expected: string | null): this {
    expect(this.getResponse().street, 'Profile street').toBe(expected);
    return this;
  }

  postalCode(expected: string | null): this {
    expect(this.getResponse().postalCode, 'Profile postalCode').toBe(expected);
    return this;
  }

  city(expected: string | null): this {
    expect(this.getResponse().city, 'Profile city').toBe(expected);
    return this;
  }

  country(expected: string | null): this {
    expect(this.getResponse().country, 'Profile country').toBe(expected);
    return this;
  }
}
