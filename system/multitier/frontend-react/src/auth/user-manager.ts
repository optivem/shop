import { InMemoryWebStorage, UserManager, WebStorageStateStore } from 'oidc-client-ts';
import { getAuthConfig, getAuthority } from './config';
import { setAccessTokenSource, setUnauthorizedHandler } from './access-token';

const config = getAuthConfig();
const origin = globalThis.location.origin;

// Authorization code flow with PKCE (S256) for the public client. Tokens live in memory only;
// the short-lived login state (state/PKCE verifier) uses the default sessionStorage so it survives the redirect.
export const userManager = new UserManager({
  authority: getAuthority(config),
  client_id: config.clientId,
  redirect_uri: `${origin}/`,
  post_logout_redirect_uri: `${origin}/`,
  response_type: 'code',
  scope: 'openid profile',
  automaticSilentRenew: true,
  userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
});

setAccessTokenSource(async () => (await userManager.getUser())?.access_token);

let redirecting = false;
setUnauthorizedHandler(() => {
  if (redirecting) return;
  redirecting = true;
  void userManager.removeUser().then(() => userManager.signinRedirect());
});
