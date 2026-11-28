import { InMemoryWebStorage, UserManager, WebStorageStateStore } from 'oidc-client-ts';
import { getAuthConfig, getAuthority, type AuthConfig } from './config';
import { setAccessTokenSource, setUnauthorizedHandler } from './access-token';
import { currentPath } from './return-url';

const config = getAuthConfig();
const origin = globalThis.location.origin;

function createUserManager(settings: AuthConfig): UserManager {
  // Authorization code flow with PKCE (S256) for the public client. Tokens live in memory only;
  // the short-lived login state (state/PKCE verifier) uses the default sessionStorage so it survives the redirect.
  return new UserManager({
    authority: getAuthority(settings),
    client_id: settings.clientId,
    redirect_uri: `${origin}/`,
    post_logout_redirect_uri: `${origin}/`,
    response_type: 'code',
    scope: 'openid profile',
    automaticSilentRenew: true,
    // The silent session check on page load must not hold the header back for long when Keycloak can't answer.
    silentRequestTimeoutInSeconds: 5,
    userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
  });
}

// Undefined when authentication is disabled (no Keycloak configured).
export const userManager = config ? createUserManager(config) : undefined;

/**
 * Wakes the auth server as soon as the app loads: the demo deployment scales Keycloak to zero, so waiting
 * for the first sign-in click to start it makes "Redirecting to sign in..." hang on a cold start. Fetches the
 * OIDC discovery document through the sign-in's own client, which caches it for the later redirect.
 * Undefined when authentication is disabled.
 */
export const authServerWarmUp: Promise<void> | undefined = userManager?.metadataService.getMetadata().then(() => undefined);
// The wake-up card reports a failure; nothing else waits on it, so it must not surface as an unhandled rejection.
authServerWarmUp?.catch(() => undefined);

if (userManager) {
  setAccessTokenSource(async () => (await userManager.getUser())?.access_token);

  let redirecting = false;
  setUnauthorizedHandler(() => {
    if (redirecting) return;
    redirecting = true;
    void userManager.removeUser().then(() => userManager.signinRedirect({ state: { returnUrl: currentPath() } }));
  });
}
