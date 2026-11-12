import type { UpdateProfileRequest } from '../../../../port/dtos/UpdateProfileRequest.js';
import type { ViewProfileResponse } from '../../../../port/dtos/ViewProfileResponse.js';
import { BasePage, PAGE_TIMEOUT_MS } from './BasePage.js';

const SAVE_PROFILE_SELECTOR = "button[aria-label='Save Profile']";
/** The profile has loaded (the single-page UIs read it first). */
export const PROFILE_READY_SELECTOR = SAVE_PROFILE_SELECTOR;
const FIRST_NAME_SELECTOR = "input[aria-label='First Name']";
const LAST_NAME_SELECTOR = "input[aria-label='Last Name']";
const EMAIL_SELECTOR = "input[aria-label='Email']";
const STREET_SELECTOR = "input[aria-label='Street']";
const POSTAL_CODE_SELECTOR = "input[aria-label='Postal Code']";
const CITY_SELECTOR = "input[aria-label='City']";
const COUNTRY_SELECTOR = "select[aria-label='Profile Country']";
const COUNTRY_OPTION_SELECTOR = `${COUNTRY_SELECTOR} option`;
const TAX_NOTICE_SELECTOR = "[aria-label='Tax Country Hint']";

/**
 * The profile `/account` (My account › Profile & address, UC-010): the name, the email from the sign-in account
 * (read-only), the default shipping address and its country select, and a note on how tax is charged. Saving shows a
 * notification: "Your profile has been saved.", or the validation error as `field: message` lines.
 */
export class ProfilePage extends BasePage {
  async waitForLoaded(): Promise<void> {
    await this.page.locator(PROFILE_READY_SELECTOR).waitFor({ state: 'visible', timeout: PAGE_TIMEOUT_MS });
  }

  /** Enters every field; a country the select does not offer is added to it, as an edited form would send it. */
  async input(request: UpdateProfileRequest): Promise<void> {
    await this.fill(FIRST_NAME_SELECTOR, request.firstName ?? '');
    await this.fill(LAST_NAME_SELECTOR, request.lastName ?? '');
    await this.fill(STREET_SELECTOR, request.street ?? '');
    await this.fill(POSTAL_CODE_SELECTOR, request.postalCode ?? '');
    await this.fill(CITY_SELECTOR, request.city ?? '');
    await this.selectOptionByValueAddingIfMissing(COUNTRY_SELECTOR, request.country ?? '');
  }

  async clickSaveProfile(): Promise<void> {
    await this.page.locator(SAVE_PROFILE_SELECTOR).click({ timeout: PAGE_TIMEOUT_MS });
  }

  /** The profile as the page shows it now; an empty field reads as "". */
  async read(): Promise<ViewProfileResponse> {
    await this.waitForLoaded();
    const email = this.page.locator(EMAIL_SELECTOR);
    const emailReadOnly = (await email.getAttribute('readonly')) !== null || (await email.getAttribute('disabled')) !== null;
    const countryChoices: string[] = [];
    for (const option of await this.page.locator(COUNTRY_OPTION_SELECTOR).all()) {
      const value = await option.getAttribute('value');
      if (value) countryChoices.push(value);
    }
    const taxNotice = (await this.page.locator(TAX_NOTICE_SELECTOR).textContent({ timeout: PAGE_TIMEOUT_MS })) ?? '';
    return {
      firstName: await this.value(FIRST_NAME_SELECTOR),
      lastName: await this.value(LAST_NAME_SELECTOR),
      email: await this.value(EMAIL_SELECTOR),
      street: await this.value(STREET_SELECTOR),
      postalCode: await this.value(POSTAL_CODE_SELECTOR),
      city: await this.value(CITY_SELECTOR),
      country: await this.value(COUNTRY_SELECTOR),
      emailChangeable: !emailReadOnly,
      countryChoices,
      taxNotice: taxNotice.trim().replaceAll(/\s+/g, ' '),
    };
  }

  private async fill(selector: string, value: string): Promise<void> {
    await this.page.locator(selector).fill(value, { timeout: PAGE_TIMEOUT_MS });
  }

  private async value(selector: string): Promise<string> {
    return this.page.locator(selector).inputValue({ timeout: PAGE_TIMEOUT_MS });
  }
}
