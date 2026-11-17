export const PROFILE_VARIABLE = 'APP_PROFILE';
export const PROD_PROFILE = 'prod';
export const REAL_MODE = 'real';
export const TEST_DB_PASSWORD = 'app';

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
  const dbPassword = env.POSTGRES_DB_PASSWORD;
  if (isBlank(dbPassword) || dbPassword === TEST_DB_PASSWORD) {
    problems.push('POSTGRES_DB_PASSWORD is missing or the test default');
  }
  const issuer = env.AUTH_ISSUER_URI;
  if (isBlank(issuer) || isLocalhost(issuer)) {
    problems.push('AUTH_ISSUER_URI is missing or points at localhost');
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
 * With APP_PROFILE=prod (set by the production deploy only), refuses to start on a missing or
 * test-default secret or endpoint. Local and pipeline stacks run without the profile, so their
 * test defaults keep working. Parity with the Java backends' prod-profile guard.
 */
export function ensureSafeForProduction(env: Env = process.env): void {
  if (!isProdProfile(env)) {
    return;
  }
  const problems = productionConfigProblems(env);
  if (problems.length > 0) {
    throw new Error(
      `Refusing to start with the prod profile: ${problems.join('; ')}`,
    );
  }
}
