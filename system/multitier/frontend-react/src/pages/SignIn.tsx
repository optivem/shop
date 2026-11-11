import { useContext, useEffect, useRef } from 'react';
import { useSearchParams, Navigate } from 'react-router-dom';
import { AuthContext } from 'react-oidc-context';
import { AuthDisabledContext } from '../auth/auth-disabled';
import { safeReturnUrl } from '../auth/return-url';

/** `/sign-in?returnUrl=…`: starts the Keycloak login and comes back to `returnUrl` (default `/`). */
export function SignIn() {
  const [params] = useSearchParams();
  const returnUrl = safeReturnUrl(params.get('returnUrl'));
  const authDisabled = useContext(AuthDisabledContext);
  const auth = useContext(AuthContext);
  const started = useRef(false);

  const signedIn = authDisabled || !auth || auth.isAuthenticated;

  useEffect(() => {
    if (signedIn || !auth || auth.isLoading || started.current) return;
    started.current = true;
    void auth.signinRedirect({ state: { returnUrl } });
  }, [auth, signedIn, returnUrl]);

  if (signedIn) return <Navigate to={returnUrl} replace />;

  // Only shown for the moment before the browser leaves for the Keycloak login page.
  return (
    <div className="signin-redirect">
      <div className="market-panel signin-redirect-panel text-center">
        <div className="spinner-border text-primary mb-3" aria-hidden="true" />
        <div className="signin-redirect-message" role="status">
          Redirecting to sign in...
        </div>
        <p className="signin-redirect-hint mb-0">You'll continue to our secure sign-in page and come straight back here.</p>
      </div>
    </div>
  );
}
