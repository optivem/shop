import {
  ensureSafeForProduction,
  productionConfigProblems,
} from './production-config.guard';

describe('production config guard', () => {
  it('accepts real values', () => {
    expect(
      productionConfigProblems({
        POSTGRES_DB_PASSWORD: 's3cret',
        AUTH_ISSUER_URI: 'https://auth.example.com/realms/shop',
      }),
    ).toEqual([]);
  });

  it('rejects test defaults and missing values', () => {
    expect(
      productionConfigProblems({
        POSTGRES_DB_PASSWORD: 'app',
        AUTH_ISSUER_URI: 'http://localhost:8191/realms/shop',
      }),
    ).toEqual([
      'POSTGRES_DB_PASSWORD is missing or the test default',
      'AUTH_ISSUER_URI is missing or points at localhost',
    ]);
    expect(productionConfigProblems({})).toHaveLength(2);
  });

  it('only checks under the prod profile', () => {
    expect(() => ensureSafeForProduction({})).not.toThrow();
    expect(() => ensureSafeForProduction({ APP_PROFILE: 'prod' })).toThrow(
      'Refusing to start with the prod profile',
    );
  });
});
