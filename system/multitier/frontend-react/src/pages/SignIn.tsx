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

  return (
    <div className="container mt-4" role="status">
      Redirecting to sign in...
    </div>
  );
}
