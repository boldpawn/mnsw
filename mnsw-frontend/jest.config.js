module.exports = {
  preset: 'jest-preset-angular',
  testEnvironment: 'jest-preset-angular/build/environments/jest-jsdom-env.js',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testMatch: ['**/src/**/*.spec.ts'],
  transform: {
    '^.+\\.(ts|js|mjs|html|svg)$': [
      'jest-preset-angular',
      {
        tsconfig: '<rootDir>/tsconfig.spec.json',
        stringifyContentPathRegex: '\\.(html|svg)$',
      },
    ],
  },
  moduleNameMapper: {
    '^@env/(.*)$': '<rootDir>/src/environments/$1',
  },
};
