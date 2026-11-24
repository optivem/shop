import type { Config } from 'jest';

const config: Config = {
  moduleFileExtensions: ['js', 'json', 'ts'],
  rootDir: 'src',
  testRegex: '.*\\.spec\\.ts$',
  testPathIgnorePatterns: ['/node_modules/', '\\.integration\\.spec\\.ts$'],
  transform: {
    '^.+\\.ts$': 'ts-jest',
    // cockatiel 4 ships ESM only; transpile it to CommonJS for Jest (`.` matches either path separator).
    'node_modules.cockatiel.+\\.js$': [
      'ts-jest',
      { tsconfig: { allowJs: true, module: 'commonjs', moduleResolution: 'node10' } },
    ],
  },
  transformIgnorePatterns: ['/node_modules/(?!cockatiel/)'],
  testEnvironment: 'node',
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/$1',
  },
};

export default config;
