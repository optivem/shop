import { useContext } from 'react';
import { AuthContext } from 'react-oidc-context';
import { AuthDisabledContext } from './auth-disabled';
import { SessionCheckedContext } from './session-check';

export type SessionStatus = 'checking' | 'anonymous' | 'signed-in';

export interface Session {
  status: SessionStatus;
  username?: string;
  /** Profile names from the sign-in account, used to prefill an empty profile or address. */
  givenName?: string;
  familyName?: string;
  signOut: () => void;
}

/** The visitor's sign-in state for the header and pages; with auth disabled everyone counts as signed in. */
export function useSession(): Session {
  const authDisabled = useContext(AuthDisabledContext);
  const sessionChecked = useContext(SessionCheckedContext);
  const auth = useContext(AuthContext);

  if (authDisabled) return { status: 'signed-in', signOut: () => undefined };
  if (!auth) return { status: 'anonymous', signOut: () => undefined };
  if (auth.isAuthenticated) {
    const profile = auth.user?.profile;
    return {
      status: 'signed-in',
      username: profile?.preferred_username ?? profile?.name ?? profile?.sub,
      givenName: profile?.given_name,
      familyName: profile?.family_name,
      signOut: () => void auth.signoutRedirect(),
    };
  }
  if (!sessionChecked || auth.isLoading || auth.activeNavigator) return { status: 'checking', signOut: () => undefined };
  return { status: 'anonymous', signOut: () => undefined };
}
