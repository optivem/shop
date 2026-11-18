export const PROFILE_VARIABLE = 'APP_PROFILE';
export const PROD_PROFILE = 'prod';
export const REAL_MODE = 'real';
export const TEST_DB_PASSWORD = 'app';
export const TEST_CLIENT_SECRET = 'shop-monolith-test-secret';
export const TEST_SESSION_SECRET = 'shop-monolith-test-session-secret-change-me';

type Env = Record<string, string | undefined>;

function isBlank(value: string | undefined): value is undefined {
  return value === undefined || value.trim() === '';
}

function isLocalhost(uri: string): boolean {
  try {
    const host = new URL(uri).hostname;
    return host === 'localhost' || host === '127.0.0.1';
  } catch {
    return true;
  }
}

/** True under APP_PROFILE=prod, which only the production deploy sets. */
export function isProdProfile(env: Env = process.env): boolean {
  return env[PROFILE_VARIABLE] === PROD_PROFILE;
}

/** Every missing or test-default setting that must not reach production. */
export function productionConfigProblems(env: Env): string[] {
  const problems: string[] = [];
  const {
    POSTGRES_DB_PASSWORD: dbPassword,
    AUTH_ISSUER_URI: issuer,
    AUTH_CLIENT_SECRET: clientSecret,
    AUTH_SESSION_SECRET: sessionSecret,
  } = env;
  if (isBlank(dbPassword) || dbPassword === TEST_DB_PASSWORD) {
    problems.push('POSTGRES_DB_PASSWORD is missing or the test default');
  }
  if (isBlank(issuer) || isLocalhost(issuer)) {
    problems.push('AUTH_ISSUER_URI is missing or points at localhost');
  }
  if (isBlank(clientSecret) || clientSecret === TEST_CLIENT_SECRET) {
    problems.push('AUTH_CLIENT_SECRET is missing or the test-realm secret');
  }
  // REL #76: the session secret seals the session and login cookies; the committed default would let anyone forge one.
  if (isBlank(sessionSecret) || sessionSecret === TEST_SESSION_SECRET) {
    problems.push('AUTH_SESSION_SECRET is missing or the test default');
  }
  // B-PRG 2.3: production reads the local system clock; the external (stub) clock exists so tests control "now".
  const externalSystemMode = env.EXTERNAL_SYSTEM_MODE;
  if (externalSystemMode !== undefined && externalSystemMode !== REAL_MODE) {
    problems.push(
      'EXTERNAL_SYSTEM_MODE is not real (prod reads the local clock; the stub clock is for test and pipeline stacks)',
    );
  }
  return problems;
}

/**
 * With APP_PROFILE=prod (set by the production deploy only), refuses to start on a missing or test-default secret or
 * endpoint. Local and pipeline stacks run without the profile, so their test defaults keep working. Parity with the
 * Java backends' prod-profile guard.
 */
export function ensureSafeForProduction(env: Env = process.env): void {
  if (!isProdProfile(env)) {
    return;
  }
  const problems = productionConfigProblems(env);
  if (problems.length > 0) {
    throw new Error(`Refusing to start with the prod profile: ${problems.join('; ')}`);
  }
}
