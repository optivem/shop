import { InMemoryWebStorage, UserManager, WebStorageStateStore } from 'oidc-client-ts';
import { getAuthConfig, getAuthority, type AuthConfig } from './config';
import { setAccessTokenSource, setUnauthorizedHandler } from './access-token';

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
    userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
  });
}

// Undefined when authentication is disabled (no Keycloak configured).
export const userManager = config ? createUserManager(config) : undefined;

if (userManager) {
  setAccessTokenSource(async () => (await userManager.getUser())?.access_token);

  let redirecting = false;
  setUnauthorizedHandler(() => {
    if (redirecting) return;
    redirecting = true;
    void userManager.removeUser().then(() => userManager.signinRedirect());
  });
}
