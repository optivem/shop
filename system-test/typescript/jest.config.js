module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  testTimeout: 60000,
  transform: {
    '^.+\\.ts$': 'ts-jest',
  },
};
