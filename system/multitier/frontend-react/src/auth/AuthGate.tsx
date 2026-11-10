import { useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { AuthProvider, hasAuthParams, useAuth } from 'react-oidc-context';
import type { User } from 'oidc-client-ts';
import { AuthDisabledContext } from './auth-disabled';
import { currentPath, returnUrlFromState } from './return-url';
import { SessionCheckedContext } from './session-check';
import { userManager } from './user-manager';

// Once the redirect callback is handled, drops `code` and `state` from the URL and moves to the page the
// visitor signed in from. The router listens to popstate, so it follows the replaced URL.
function onSigninCallback(user: User | undefined): void {
  const returnUrl = returnUrlFromState(user?.state) ?? globalThis.location.pathname;
  globalThis.history.replaceState({}, document.title, returnUrl);
  globalThis.dispatchEvent(new PopStateEvent('popstate'));
}

const inIframe = (): boolean => {
  try {
    return globalThis.self !== globalThis.top;
  } catch {
    return true;
  }
};

/**
 * Redirects unauthenticated visitors to the login page and renders children once signed in.
 * Wraps only the routes that need a signed-in user; the storefront itself is public.
 */
export function RequireAuth({ children }: Readonly<{ children: ReactNode }>) {
  const authDisabled = useContext(AuthDisabledContext);
  if (authDisabled) return <>{children}</>;
  return <RequireOidcAuth>{children}</RequireOidcAuth>;
}

function RequireOidcAuth({ children }: Readonly<{ children: ReactNode }>) {
  const auth = useAuth();
  const sessionChecked = useContext(SessionCheckedContext);
  const attempted = useRef(false);

  useEffect(() => {
    if (
      sessionChecked &&
      !hasAuthParams() &&
      !auth.isAuthenticated &&
      !auth.activeNavigator &&
      !auth.isLoading &&
      !auth.error &&
      !attempted.current
    ) {
      attempted.current = true;
      void auth.signinRedirect({ state: { returnUrl: currentPath() } });
    }
  }, [auth, sessionChecked]);

  if (auth.error) {
    return (
      <div className="container mt-4">
        <div className="alert alert-danger" role="alert">
          Sign-in failed: {auth.error.message}
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => void auth.signinRedirect({ state: { returnUrl: currentPath() } })}
        >
          Try again
        </button>
      </div>
    );
  }

  if (!auth.isAuthenticated) {
    return (
      <div className="container mt-4" role="status">
        Redirecting to sign in...
      </div>
    );
  }

  return <>{children}</>;
}

// Started once per page load (not per render), so React StrictMode's double effects don't run it twice.
let sessionCheck: Promise<void> | undefined;

function checkExistingSession(): Promise<void> {
  if (!userManager || hasAuthParams()) return Promise.resolve();
  const manager = userManager;
  sessionCheck ??= manager
    .getUser()
    .then((user) => (user && !user.expired ? undefined : manager.signinSilent().then(() => undefined)))
    // No session (login_required) or Keycloak can't be framed: the visitor is simply anonymous.
    .catch(() => undefined);
  return sessionCheck;
}

/**
 * Loads an existing Keycloak session without any visible redirect (a prompt=none request in a hidden iframe),
 * so a signed-in visitor who reloads a public page still sees the signed-in header.
 */
function SilentSession({ children }: Readonly<{ children: ReactNode }>) {
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    let active = true;
    void checkExistingSession().then(() => {
      if (active) setChecked(true);
    });
    return () => {
      active = false;
    };
  }, []);

  return <SessionCheckedContext.Provider value={checked}>{children}</SessionCheckedContext.Provider>;
}

// The hidden iframe of a silent sign-in (or token renewal) loads the app with the login response; it only
// hands that response to the parent window and renders nothing.
let iframeCallbackHandled = false;

function SilentCallback() {
  useEffect(() => {
    if (iframeCallbackHandled || !userManager) return;
    iframeCallbackHandled = true;
    void userManager.signinCallback().catch(() => undefined);
  }, []);
  return null;
}

/**
 * Provides the OIDC session to the app without forcing a login: public routes render for anonymous visitors,
 * and protected routes wrap themselves in RequireAuth. Renders the app as-is (no login, no token) when no
 * Keycloak is configured.
 */
export function AuthGate({ children }: Readonly<{ children: ReactNode }>) {
  if (!userManager) {
    return <AuthDisabledContext.Provider value={true}>{children}</AuthDisabledContext.Provider>;
  }
  if (inIframe() && hasAuthParams()) {
    return <SilentCallback />;
  }
  return (
    <AuthProvider userManager={userManager} onSigninCallback={onSigninCallback}>
      <SilentSession>{children}</SilentSession>
    </AuthProvider>
  );
}
