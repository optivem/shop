import { expect } from '@playwright/test';
import type { GetTimeResponse } from '../../../../../../driver/port/external/clock/dtos/GetTimeResponse.js';
import { ResponseVerification } from '../../../../shared/response-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';

export class GetTimeVerification extends ResponseVerification<GetTimeResponse> {
  constructor(response: GetTimeResponse, context: UseCaseContext) {
    super(response, context);
  }

  timeIsNotNull(): this {
    expect(this.getResponse().time).not.toBeNull();
    expect(this.getResponse().time).not.toBeUndefined();
    return this;
  }

  time(expectedTime: string): this {
    expect(this.getResponse().time).toBe(expectedTime);
    return this;
  }

  timeIsAfter(time: string): this {
    expect(new Date(this.getResponse().time).getTime()).toBeGreaterThan(new Date(time).getTime());
    return this;
  }

  timeIsBefore(time: string): this {
    expect(new Date(this.getResponse().time).getTime()).toBeLessThan(new Date(time).getTime());
    return this;
  }

  timeIsBetween(start: string, end: string): this {
    const actual = new Date(this.getResponse().time).getTime();
    expect(actual).toBeGreaterThanOrEqual(new Date(start).getTime());
    expect(actual).toBeLessThanOrEqual(new Date(end).getTime());
    return this;
  }
}
