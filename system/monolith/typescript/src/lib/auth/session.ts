import { EncryptJWT, jwtDecrypt } from 'jose';
import { authSettings } from './settings';

export const SESSION_COOKIE = 'myshop_session';
export const LOGIN_COOKIE = 'myshop_login';

const SESSION_TTL_SECONDS = 60 * 60;
const LOGIN_TTL_SECONDS = 10 * 60;

export interface Session {
  /** Token subject (`sub`) of the logged-in user. */
  subject: string;
  name: string;
  roles: string[];
  /** Kept only so logout can tell Keycloak which session to end. */
  idToken: string;
}

/** State of an in-flight login, carried in a short-lived encrypted cookie. */
export interface LoginState {
  state: string;
  nonce: string;
  codeVerifier: string;
  returnTo: string;
}

export const cookieOptions = (maxAge: number) => ({
  httpOnly: true,
  sameSite: 'lax' as const,
  path: '/',
  maxAge,
});

export const sessionCookieOptions = cookieOptions(SESSION_TTL_SECONDS);
export const loginCookieOptions = cookieOptions(LOGIN_TTL_SECONDS);

async function encryptionKey(): Promise<Uint8Array> {
  const secret = new TextEncoder().encode(authSettings().sessionSecret);
  return new Uint8Array(await crypto.subtle.digest('SHA-256', secret));
}

async function seal(payload: object, ttlSeconds: number): Promise<string> {
  return new EncryptJWT({ ...payload })
    .setProtectedHeader({ alg: 'dir', enc: 'A256GCM' })
    .setIssuedAt()
    .setExpirationTime(`${ttlSeconds}s`)
    .encrypt(await encryptionKey());
}

async function unseal(token: string | undefined): Promise<Record<string, unknown> | null> {
  if (!token) {
    return null;
  }
  try {
    const { payload } = await jwtDecrypt(token, await encryptionKey());
    return payload;
  } catch {
    return null;
  }
}

export const sealSession = (session: Session) => seal(session, SESSION_TTL_SECONDS);
export const sealLoginState = (login: LoginState) => seal(login, LOGIN_TTL_SECONDS);

export async function openSession(token: string | undefined): Promise<Session | null> {
  const p = await unseal(token);
  if (
    !p ||
    typeof p.subject !== 'string' ||
    typeof p.name !== 'string' ||
    typeof p.idToken !== 'string' ||
    !Array.isArray(p.roles)
  ) {
    return null;
  }
  return {
    subject: p.subject,
    name: p.name,
    idToken: p.idToken,
    roles: p.roles.filter((r): r is string => typeof r === 'string'),
  };
}

export async function openLoginState(token: string | undefined): Promise<LoginState | null> {
  const p = await unseal(token);
  if (
    !p ||
    typeof p.state !== 'string' ||
    typeof p.nonce !== 'string' ||
    typeof p.codeVerifier !== 'string' ||
    typeof p.returnTo !== 'string'
  ) {
    return null;
  }
  return { state: p.state, nonce: p.nonce, codeVerifier: p.codeVerifier, returnTo: p.returnTo };
}
