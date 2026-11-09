import { expect } from '@playwright/test';
import { UserIdentity } from '../../../../driver/port/user-identity.js';
import type { AppContext } from '../app-context.js';
import type { ProfileConfig, ScenarioContext } from '../scenario-context.js';

/** Sets up a given profile: saves it as its customer and checks that it was saved. */
export async function saveGivenProfile(app: AppContext, ctx: ScenarioContext, profile: ProfileConfig): Promise<void> {
  app.actAs(ctx.savingIdentity(profile));
  try {
    const result = await app.myShop().updateProfile({
      firstName: profile.firstName,
      lastName: profile.lastName,
      street: profile.street,
      postalCode: profile.postalCode,
      city: profile.city,
      country: profile.country,
    });
    expect(result.success, `Given profile could not be saved: ${JSON.stringify(result)}`).toBe(true);
  } finally {
    app.actAs(UserIdentity.DEFAULT);
  }
}
